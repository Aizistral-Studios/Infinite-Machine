package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.utils.StandardLogger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;

import java.util.ArrayList;
import java.util.List;

public class ExhaustiveMessageIndexer {
    private static final StandardLogger LOGGER = new StandardLogger("Exhaustive Message Indexer");

    private List<GuildMessageChannel> domainChannels;

    public ExhaustiveMessageIndexer() {
        LOGGER.log("ExhaustiveIndexer ready, awaiting orders.");
    }

    public void executeReindex() {
        LOGGER.log("Executing indexation please stand by...");
        this.domainChannels = collectGuildChannels();
        indexAllMessages();
        LOGGER.log("Fetched all messages in domain. Proceeding with database verification.");
        //cleanupIndexedMessages();
        LOGGER.log("Indexation completed. Resuming normal operations.");
    }

    // ------------------ //
    // Message Indexation //
    // ------------------ //
    private void indexAllMessages() {
        for (GuildMessageChannel channel : domainChannels) {
            indexChannelMessages(channel);
        }
    }

    private void indexChannelMessages(GuildMessageChannel channel) {
        LOGGER.log("Inspecting channel: " + channel.getName());
        int batchSize = 100;
        MessageHistory history = channel.getHistoryFromBeginning(batchSize).complete();
        if (history == null || history.isEmpty()) return;
        List<Message> messages = history.getRetrievedHistory();
        while (messages != null) {
            LOGGER.log(String.format("Length of MessageList : %d", messages.size()));
            long lastMessageID = -1;
            for (int i = messages.size() - 1; i >= 0 ; i--) {
                Message message = messages.get(i);
                CoreMessageIndexer.INSTANCE.indexMessage(message);
                lastMessageID = message.getIdLong();
            }
            try {
                messages = getNextMessages(channel, lastMessageID, batchSize);
            } catch (ErrorResponseException ex) {
                if(ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                    LOGGER.log("Lost message trail, restarting indexation process.");
                    history = channel.getHistoryFromBeginning(batchSize).complete();
                    if (history == null || history.isEmpty()) return;
                    messages = history.getRetrievedHistory();
                } else {
                    throw ex;
                }
            }
        }
    }

    private List<Message> getNextMessages(GuildMessageChannel channel, long lastMessageID, int batchSize) throws ErrorResponseException {
        MessageHistory history = channel.getHistoryAfter(lastMessageID, batchSize).complete();
        if (history == null || history.isEmpty()) return null;
        return history.getRetrievedHistory();
    }

    // ---------------- //
    // Database Cleanup //
    // ---------------- //


    // ----------------- //
    // Domain Evaluation //
    // ----------------- //
    private List<GuildMessageChannel> collectGuildChannels() {
        Guild domain = InfiniteMachine.INSTANCE.getDomain();

        List<GuildMessageChannel> channels = new ArrayList<>();

        List<TextChannel> textChannels = new ArrayList<>();
        List<ThreadChannel> threads = new ArrayList<>();
        List<VoiceChannel> voiceChannels = new ArrayList<>();

        LOGGER.log("Starting domain evaluation...");

        domain.getChannels().stream().filter(c -> c instanceof TextChannel).map(c -> (TextChannel) c).forEach(textChannels::add);
        domain.retrieveActiveThreads().complete().forEach(threads::add);
        domain.getChannels().stream().filter(c -> c instanceof VoiceChannel).map(c -> (VoiceChannel) c).forEach((voiceChannels::add));
        for (TextChannel channel : textChannels) {
            threads.addAll(this.extractAllThreads(channel.retrieveArchivedPublicThreadChannels()));
            threads.addAll(this.extractAllThreads(channel.retrieveArchivedPrivateThreadChannels()));
        }

        LOGGER.log("Domain evaluation complete.");
        LOGGER.log("Text channels found: %s", textChannels.size());
        LOGGER.log("Threads found: %s", threads.size());
        LOGGER.log("VoiceChannels found: %s", voiceChannels.size());

        channels.addAll(textChannels);
        channels.addAll(threads);
        channels.addAll(voiceChannels);

        return channels;
    }

    private List<ThreadChannel> extractAllThreads(ThreadChannelPaginationAction action) {
        List<ThreadChannel> threads = new ArrayList<>();
        action.cache(false).forEachAsync(threads::add).join();
        return threads;
    }

}
