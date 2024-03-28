package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.data.ProcessedMessage;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.database.FieldType;
import com.aizistral.infmachine.database.Table;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;

public class CoreMessageIndexer {
    private static final StandardLogger LOGGER = new StandardLogger("Core Message Indexer");

    public static final CoreMessageIndexer INSTANCE = new CoreMessageIndexer();

    private final DataBaseHandler databaseHandler;
    private final RealtimeMessageIndexer realtimeMessageIndexer;
    private final ExhaustiveMessageIndexer exhaustiveMessageIndexer;

    private CoreMessageIndexer()
    {
        this.databaseHandler = DataBaseHandler.INSTANCE;
        createMessageIndexTable();
        this.realtimeMessageIndexer = new RealtimeMessageIndexer();
        this.exhaustiveMessageIndexer = new ExhaustiveMessageIndexer();
        LOGGER.log("CoreMessageIndexer instantiated.");
    }

    public void init() {
        exhaustiveMessageIndexer.executeReindex();
    }

    // ---------------- //
    // Indexation Hooks //
    // ---------------- //
    public void indexMessage(Message message) {
        if(!isValidMessage(message)) return;
        long messageID = message.getIdLong();
        long userID = getUserOfMessage(message).getIdLong();
        long channelID = message.getChannel().getIdLong();
        long rating = evaluateMessage(message);
        String sql = String.format("INSERT INTO messageIndex (messageID, authorID, channelID, messageRating) VALUES(%d,%d,%d,%d)",messageID, userID, channelID, rating);
        databaseHandler.executeSQL(sql);
    }

    public void unindexMessage(long deletedMessageID) {
        String sql = String.format("DELETE FROM messageIndex WHERE messageID = %d", deletedMessageID);
        databaseHandler.executeSQL(sql);
    }

    // --------- //
    // Utilities //
    // --------- //

    private void createMessageIndexTable() {
        Table.Builder tableBuilder = new Table.Builder("messageIndex");
        tableBuilder.addField("messageID", FieldType.LONG, true, true);
        tableBuilder.addField("authorID", FieldType.LONG, false, true);
        tableBuilder.addField("channelID", FieldType.LONG, false, true);
        tableBuilder.addField("messageRating", FieldType.LONG, false, true);
        Table table = tableBuilder.build();
        databaseHandler.createNewTable(table);
    }

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
