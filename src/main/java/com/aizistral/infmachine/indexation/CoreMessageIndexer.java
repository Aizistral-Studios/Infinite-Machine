package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.database.FieldType;
import com.aizistral.infmachine.database.Table;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;

import java.time.OffsetDateTime;

public class CoreMessageIndexer {
    private static final StandardLogger LOGGER = new StandardLogger("Core Message Indexer");

    public static final CoreMessageIndexer INSTANCE = new CoreMessageIndexer();

    private final DataBaseHandler databaseHandler;
    private final RealtimeMessageIndexer realtimeMessageIndexer;
    private final ExhaustiveMessageIndexer exhaustiveMessageIndexer;
    private final String indexTableName = "messageIndex";

    private final long indexationTimeTail = OffsetDateTime.now().toEpochSecond() - 1 * 1 * 24 * 60 * 60;

    private CoreMessageIndexer()
    {
        this.databaseHandler = DataBaseHandler.INSTANCE;
        prepareDatabase();
        this.realtimeMessageIndexer = new RealtimeMessageIndexer();
        this.exhaustiveMessageIndexer = new ExhaustiveMessageIndexer(
            () -> {
                InfiniteMachine.INSTANCE.getMachineChannel().sendMessage(String.format("Convergence achieved. All data accounted for.")).queue();
            },
            () -> {
                InfiniteMachine.INSTANCE.getMachineChannel().sendMessage(String.format("Indexer encountered critical error. Please restart process.")).queue();
            }
        );
        LOGGER.log("CoreMessageIndexer instantiated.");
    }

    public String getIndexTableName() {
        return indexTableName;
    }

    public void startExhaustiveIndexRunner() {
        Thread exhaustiveIndexer = new Thread(exhaustiveMessageIndexer, "ExhaustiveIndexer-Thread");
        exhaustiveIndexer.start();
    }

    public long getIndexationTimeTail() {
        return indexationTimeTail;
    }

    // ---------------- //
    // Indexation Hooks //
    // ---------------- //
    public void indexMessage(Message message) {
        long messageID = message.getIdLong();
        long userID = isValidMessage(message) ? getUserOfMessage(message).getIdLong() : -1L;
        long channelID = message.getChannel().getIdLong();
        long rating = evaluateMessage(message);
        long time = message.getTimeCreated().toEpochSecond();
        String sql = String.format("REPLACE INTO %s (messageID, authorID, channelID, messageRating, timeStamp) VALUES(%d,%d,%d,%d,%d)", indexTableName,messageID, userID, channelID, rating, time);
        databaseHandler.executeSQL(sql);
    }

    public void unindexMessage(long deletedMessageID) {
        String sql = String.format("DELETE FROM %s WHERE messageID = %d", indexTableName, deletedMessageID);
        databaseHandler.executeSQL(sql);
    }

    // --------------- //
    // Database Access //
    // --------------- //
    private void prepareDatabase() {
        createMessageIndexTable();
        removeMessagesNewerThen(indexationTimeTail);
    }

    private void createMessageIndexTable() {
        Table.Builder tableBuilder = new Table.Builder(indexTableName);
        tableBuilder.addField("messageID", FieldType.LONG, true, true);
        tableBuilder.addField("authorID", FieldType.LONG, false, true);
        tableBuilder.addField("channelID", FieldType.LONG, false, true);
        tableBuilder.addField("messageRating", FieldType.LONG, false, true);
        tableBuilder.addField("timeStamp", FieldType.TIME, false, true);
        Table table = tableBuilder.build();
        databaseHandler.createNewTable(table);
    }

    private void removeMessagesNewerThen(long dateTimeInSeconds) {
        String sql = String.format("DELETE FROM %s WHERE timeStamp > %d", indexTableName, dateTimeInSeconds);
        databaseHandler.executeSQL(sql);
    }

    // --------- //
    // Utilities //
    // --------- //
    private User getUserOfMessage(Message message) {
        return message.getInteraction() != null ? message.getInteraction().getUser() : message.getAuthor();
    }

    private boolean isValidMessage(Message message) {
        User user = getUserOfMessage(message);
        if (user.isBot() || user.isSystem()) return false;
        if (user.getIdLong() == Utils.DELETED_USER_ID) return false;
        return true;
    }

    // TODO Test for Voice-messages :: Possibly add content evaluation (Filter for word variety)
    // TODO change scope to private once the indexation rework is complete
    public static long evaluateMessage(Message messageRaw) {
        if (messageRaw.getType().equals(MessageType.SLASH_COMMAND)) return 0;

        // linkLengthValueInChars describes the flat amount of chars that a Link will
        // contribute to a message Rating
        long linkLengthValueInChars = 25;

        ProcessedMessage message = new ProcessedMessage(messageRaw);
        long linkContributionToLength = message.getLinkCount() * linkLengthValueInChars;
        long emojiContributionToLength = message.getEmojiCount();
        long length = message.getUniqueLength() + linkContributionToLength + emojiContributionToLength;

        return length * length;
    }
}
