package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.utils.StandardLogger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExhaustiveMessageIndexer implements Runnable{
    private static final StandardLogger LOGGER = new StandardLogger("Exhaustive Indexer");

    private List<GuildMessageChannel> domainChannels;
    private final Runnable callbackOnSuccess;
    private final Runnable callbackOnFailure;
    private volatile boolean isFullIndex;

    private volatile boolean continueRunning = true;

    public ExhaustiveMessageIndexer(Runnable callbackOnSuccess, Runnable callbackOnFailure) {
        this.callbackOnSuccess = callbackOnSuccess;
        this.callbackOnFailure = callbackOnFailure;
        //sets the full index to true on the very first indexation run
        // and then sets the database into a state where the first indexation has been completed
        // once the indexation runner has successfully indexed for the first time
        setFullIndex(!DataBaseHandler.INSTANCE.retrievePrimalIndexation());
        LOGGER.log("ExhaustiveIndexer ready, awaiting orders.");
    }
    @Override
    public void run() {
        this.continueRunning = true;
        try{
            InfiniteConfig.INSTANCE.getMachineChannel().sendMessage("Starting Indexation...").complete();
            executeReindex();
            callbackOnSuccess.run();
            LOGGER.log("Indexation completed. Full success");
        } catch(Exception ex) {
            callbackOnFailure.run();
            LOGGER.error("Indexer experienced Fatal Error:" + Arrays.toString(ex.getStackTrace()));
        }
    }

    public void stop() {
        this.continueRunning = false;
        LOGGER.log("Terminating indexation thread");
    }

    public void executeReindex() {
        LOGGER.log("Executing indexation please stand by...");
        this.domainChannels = collectGuildChannels(isFullIndex);
        indexAllMessages(isFullIndex);
        LOGGER.log("Indexation completed. Resuming normal operations.");
        setFullIndex(false);
        DataBaseHandler.INSTANCE.setPrimalIndexation();
    }

    public void setFullIndex(boolean isFullIndex) {
        this.isFullIndex = isFullIndex;
    }

    // ------------------ //
    // Message Indexation //
    // ------------------ //
    private void indexAllMessages(Boolean fullIndex) {
        int i = 0;
        for (GuildMessageChannel channel : domainChannels) {
            if(!continueRunning) return;
            LOGGER.log(String.format("%d of %d", ++i, domainChannels.size()));
            if(fullIndex) indexAllChannelMessages(channel);
            else indexChannelMessagesAfterMessage(channel, getNewestIndexedMessageIDOlderThenTimeStamp(channel.getIdLong(), CoreMessageIndexer.INSTANCE.getIndexationTimeTail()));
        }
    }

    private void indexAllChannelMessages(GuildMessageChannel channel) {
        indexChannelMessagesAfterMessage(channel, -1L);
    }

    private void indexChannelMessagesAfterMessage(GuildMessageChannel channel, long lastMessageID) {
        LOGGER.log("Inspecting channel: " + channel.getName());
        List<Message> messages = getNextMessages(channel, lastMessageID);
        while(messages != null) {
            LOGGER.log(String.format("Length of MessageList : %d", messages.size()));
            for (int i = messages.size() - 1; i >= 0 ; i--) {
                if(!continueRunning) {
                    LOGGER.log("Termination signal received. Shutting down indexing thread.");
                    return;
                }
                Message message = messages.get(i);
                CoreMessageIndexer.INSTANCE.indexMessage(message);
                lastMessageID = message.getIdLong();
            }
            messages = getNextMessages(channel, lastMessageID);
        }
    }

    private List<Message> getNextMessages(GuildMessageChannel channel, long lastMessageID) {
        while(true) {
            try {
                MessageHistory history;
                int batchSize = 100;
                if(lastMessageID < 0){
                    history = channel.getHistoryFromBeginning(batchSize).complete();
                } else {
                    history = channel.getHistoryAfter(lastMessageID, batchSize).complete();
                }
                if (history == null || history.isEmpty()) return null;
                return history.getRetrievedHistory();
            } catch(ErrorResponseException ex) {
                if(ex.getErrorResponse() == ErrorResponse.SERVER_ERROR) {
                    LOGGER.log("Sever send error response. Trying again...");
                } else if(ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                    LOGGER.log("Lost message trail. Restarting at last indexed message...");
                    CoreMessageIndexer.INSTANCE.unindexMessage(lastMessageID);
                    lastMessageID = recoverLastMessageOlderThenID(channel.getIdLong(), lastMessageID);
                } else {
                    LOGGER.error(ex.getMessage() + 1);
                    System.exit(1);
                }
            }
        }
    }

    private long getNewestIndexedMessageIDOlderThenTimeStamp(long channelID, long timestamp) {
        if(timestamp < 0) return recoverLastMessageOlderThenID(channelID, -1L);
        String sql = String.format("SELECT * FROM %s WHERE channelID = %d AND timeStamp <= %d ORDER BY messageID DESC LIMIT 1;", CoreMessageIndexer.INSTANCE.getIndexTableName(),channelID, timestamp);
        List<Map<String, Object>> results = DataBaseHandler.INSTANCE.executeQuerySQL(sql);
        if(results.isEmpty()) return -1L;
        return ((long) results.get(0).get("messageID"));
    }

    private long recoverLastMessageOlderThenID(long channelID, long messageID) {
        String sql = String.format("SELECT * FROM %s WHERE channelID = %d%s ORDER BY timeStamp DESC, messageID DESC LIMIT 1;", CoreMessageIndexer.INSTANCE.getIndexTableName(),channelID,  messageID > 0 ? " AND messageID < " + messageID : "");
        List<Map<String, Object>> results = DataBaseHandler.INSTANCE.executeQuerySQL(sql);
        if(results.isEmpty()) return -1L;
        return ((long) results.get(0).get("messageID"));
    }

    // ----------------- //
    // Domain Evaluation //
    // ----------------- //
    private List<GuildMessageChannel> collectGuildChannels(boolean fullIndex) {
        Guild domain = InfiniteConfig.INSTANCE.getDomain();

        List<GuildMessageChannel> channels = new ArrayList<>();

        List<TextChannel> textChannels = new ArrayList<>();
        List<VoiceChannel> voiceChannels = new ArrayList<>();
        List<StageChannel> stageChannels = new ArrayList<>();

        LOGGER.log("Starting domain evaluation...");

        domain.getChannels().stream().filter(c -> c instanceof TextChannel).map(c -> (TextChannel) c).forEach(textChannels::add);
        List<ThreadChannel> threads = new ArrayList<>(domain.retrieveActiveThreads().complete());
        domain.getChannels().stream().filter(c -> c instanceof VoiceChannel).map(c -> (VoiceChannel) c).forEach((voiceChannels::add));
        domain.getChannels().stream().filter(c -> c instanceof StageChannel).map(c -> (StageChannel) c).forEach((stageChannels::add));

        if(fullIndex) {
            for (TextChannel channel : textChannels) {
                threads.addAll(this.extractAllThreads(channel.retrieveArchivedPublicThreadChannels()));
                threads.addAll(this.extractAllThreads(channel.retrieveArchivedPrivateThreadChannels()));
            }
        }

        LOGGER.log("Domain evaluation complete.");
        LOGGER.log("TextChannels found: %s", textChannels.size());
        LOGGER.log("Threads found: %s", threads.size());
        LOGGER.log("VoiceChannels found: %s", voiceChannels.size());
        LOGGER.log("StageChannels found: %s", stageChannels.size());

        channels.addAll(textChannels);
        channels.addAll(threads);
        channels.addAll(voiceChannels);
        channels.addAll(stageChannels);

        return channels;
    }

    private List<ThreadChannel> extractAllThreads(ThreadChannelPaginationAction action) {
        List<ThreadChannel> threads = new ArrayList<>();
        action.cache(false).forEachAsync(threads::add).join();
        return threads;
    }


}
