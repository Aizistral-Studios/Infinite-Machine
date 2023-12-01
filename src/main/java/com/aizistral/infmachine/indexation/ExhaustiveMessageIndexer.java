package com.aizistral.infmachine.indexation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.data.ChannelType;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;

public class ExhaustiveMessageIndexer {
    private static final StandardLogger LOGGER = new StandardLogger("ExhaustiveMessageIndexer");

    private final List<Runnable> convergenceHandlers;
    private final AtomicBoolean halt, active;
    private final MachineDatabase database;
    private final Guild guild;
    private final int minMessageLength;
    private Thread thread;

    public ExhaustiveMessageIndexer(Guild guild, MachineDatabase database, int minMessageLength) {
        this.minMessageLength = minMessageLength;
        this.convergenceHandlers = new ArrayList<>();
        this.guild = guild;
        this.database = database;
        this.halt = new AtomicBoolean(false);
        this.active = new AtomicBoolean(false);
    }

    private List<ThreadChannel> extractAll(ThreadChannelPaginationAction action) {
        List<ThreadChannel> threads = new ArrayList<>();
        action.cache(false).forEachAsync(threads::add).join();
        return threads;
    }

    private void coreLoop() {
        LOGGER.log("Starting up exhaustive message indexer...");
        this.active.set(true);
        int iteration = 0;

        while (true) {
            int indexedMessages = 0;
            LOGGER.log("Starting iteration %s of exhaustive indexation...", iteration);

            List<GuildMessageChannel> channels = new ArrayList<>();
            List<TextChannel> textChannels = new ArrayList<>();
            List<ThreadChannel> threads = new ArrayList<>();

            LOGGER.log("Starting initial domain evaluation...");

            this.guild.getChannels().stream().filter(c -> c instanceof TextChannel).map(c -> (TextChannel) c)
            .forEach(textChannels::add);
            this.guild.retrieveActiveThreads().complete().forEach(threads::add);

            if (iteration < 1) {
                // Only get archived threads in first iteration, since if they remain archived -
                // that likely means that no new messages were added to them
                for (TextChannel channel : textChannels) {
                    threads.addAll(this.extractAll(channel.retrieveArchivedPublicThreadChannels()));
                    threads.addAll(this.extractAll(channel.retrieveArchivedPrivateThreadChannels()));
                }
            }

            LOGGER.log("Initial domain evaluation complete.");
            LOGGER.log("Text channels found: %s", textChannels.size());
            LOGGER.log("Threads found: %s", threads.size());

            channels.addAll(textChannels);
            channels.addAll(threads);

            for (GuildMessageChannel channel : channels) {
                LOGGER.log("Inspecting channel: " + channel.getName());

                ChannelType channelType = ChannelType.NORMAL;

                if (channel instanceof ThreadChannel) {
                    channelType = ChannelType.THREAD;
                }

                int cycleCounter = 0;

                while (true) {
                    LOGGER.debug("Hit cycle " + cycleCounter + "...");

                    if (this.halt.get()) {
                        LOGGER.log("Received halt command, saving database.");
                        this.database.forceSave();
                        this.active.set(false);
                        return;
                    }

                    long channelID = channel.getIdLong();
                    long lastMessageID = -1;

                    if (this.database.hasIndexedMessages(channelType, channelID)) {
                        int index = 0;

                        while (true) {
                            lastMessageID = this.database.getIndexedMessage(channelType, channelID, index++);

                            if (lastMessageID <= 0) {
                                break;
                            }

                            Message lastMessage = null;

                            try {
                                lastMessage = channel.retrieveMessageById(lastMessageID).complete();
                            } catch (ErrorResponseException ex) {
                                if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                                    // Ignore UNKNOWN_MESSAGE since it means message was deleted
                                    LOGGER.error("Lost message marker %s!", lastMessageID);
                                } else throw ex;
                            }

                            if (lastMessage != null) {
                                break; // Message exists, lastMessageID is valid
                            }
                        }
                    }

                    MessageHistory history = null;

                    try {
                        history = lastMessageID > 0 ? channel.getHistoryAfter(lastMessageID, 100).complete()
                                : channel.getHistoryFromBeginning(100).complete();
                    } catch (ErrorResponseException ex) {
                        if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                            continue;
                        } else throw ex;
                    }

                    if (history == null || history.isEmpty()) {
                        break;
                    }

                    List<Message> messages = history.getRetrievedHistory();

                    for (int i = 0; i < messages.size(); i++) {
                        Message message = messages.get(messages.size() - 1 - i); // list starts with latest msgs
                        User author = message.getAuthor();

                        if (message.getInteraction() != null) {
                            author = message.getInteraction().getUser();
                        }

                        if (!author.isSystem() && !author.isBot()) {
                            if (author.getIdLong() != Utils.DELETED_USER_ID) {
                                if(!(message.getContentRaw().length() >= minMessageLength)){
                                    this.database.addMessageCount(author.getIdLong(), 1);
                                }
                            }
                        }

                        if (i == (messages.size() - 1)) {
                            LOGGER.debug("Updated last message: " + message.getIdLong()
                            + ", from: " + author.getEffectiveName() + ", at: " + message.getTimeCreated()
                            .toString());

                            this.database.addIndexedMessage(channelType, channel.getIdLong(),
                                    message.getIdLong());
                        }

                        indexedMessages++;
                    }

                    cycleCounter++;
                }
            }

            LOGGER.log("Iteration %s of exhaustive indexation complete.", iteration);
            LOGGER.log("Messages indexed in this iteration: %s", indexedMessages);

            if (indexedMessages <= 0) {
                LOGGER.log("Convergence achieved.");
                this.convergenceHandlers.forEach(Runnable::run);
                break;
            } else {
                iteration++;
            }
        }

        this.active.set(false);
        LOGGER.log("The deed is done.");
    }

    public synchronized void activate() {
        if (this.thread == null) {
            this.thread = new Thread(this::coreLoop);
            this.thread.setName("ExhaustiveMessageIndexer");
            this.thread.start();
        } else {
            InfiniteMachine.INSTANCE.terminate(new RuntimeException("Cannot activate: already running"));
        }
    }

    public synchronized boolean isActive() {
        return this.thread != null && this.active.get();
    }

    public synchronized boolean isFinished() {
        return this.thread != null && !this.active.get();
    }

    public synchronized void halt() {
        this.halt.set(true);
    }

    public void onConvergence(Runnable run) {
        this.convergenceHandlers.add(run);
    }

}
