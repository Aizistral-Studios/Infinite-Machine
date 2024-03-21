package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.data.ProcessedMessage;
import com.aizistral.infmachine.database.local.IndexedMessageDatabase;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

public class CoreMessageIndexer {
    private static final StandardLogger LOGGER = new StandardLogger("Core Message Indexer");
    private RealtimeMessageIndexer realtimeMessageIndexer;
    private ExhaustiveMessageIndexer exhaustiveMessageIndexer;
    private IndexedMessageDatabase database = IndexedMessageDatabase.INSTANCE;

    public static final CoreMessageIndexer INSTANCE = new CoreMessageIndexer();

    private CoreMessageIndexer()
    {
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
    public void indexMessage(Message message)
    {
        if(!isValidMessage(message)) return;
        long messageID = message.getIdLong();
        long userID = getUserOfMessage(message).getIdLong();
        long rating = evaluateMessage(message);

        database.setCachedMessageByID(messageID,userID, rating);
    }

    public void unindexMessage(long deletedMessageID)
    {
        List<Long> cachedEntry = database.getCachedMessageByID(deletedMessageID);

        if(!cachedEntry.isEmpty())
        {
            //ToDo Link to ScoreboardDatabase to update relevant data
            //this.database.addMessageCount(cachedEntry.get(0), -1);
            //this.database.addMessageRating(cachedEntry.get(0), -Math.toIntExact(cachedEntry.get(1)));
            database.removeCachedMessageByID(deletedMessageID);
        }
    }

    // --------- //
    // Utilities //
    // --------- //

    private User getUserOfMessage(Message message)
    {
        return message.getInteraction() != null ? message.getInteraction().getUser() : message.getAuthor();
    }

    private boolean isValidMessage(Message message)
    {
        User user = getUserOfMessage(message);
        if (user.isBot() || user.isSystem()) return false;
        if (user.getIdLong() == Utils.DELETED_USER_ID) return false;
        return true;
    }

    // TODO Test for Voice-messages :: Possibly add content evaluation (Filter for word variety)
    // TODO change scope to private once the indexation rework is complete
    public static long evaluateMessage(Message messageRaw) {
        if (messageRaw.getType().equals(MessageType.SLASH_COMMAND))
            return 0;

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
