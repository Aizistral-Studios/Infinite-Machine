package com.aizistral.infmachine.indexation;

import com.aizistral.infmachine.data.ProcessedMessage;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;

public class CoreMessageIndexer {
    protected static final StandardLogger LOGGER = new StandardLogger("Message Indexer");
    public static final CoreMessageIndexer INSTANCE = new CoreMessageIndexer();

    private CoreMessageIndexer()
    {
        LOGGER.log("CoreMessageIndexer instantiated.");
    }

    public void indexMessage(Message message)
    {

    }

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
    public static int evaluateMessage(Message messageRaw) {
        // Exclude slash commands from rating
        if (messageRaw.getType().equals(MessageType.SLASH_COMMAND))
            return 0;

        // linkLengthValueInChars describes the flat amount of chars that a Link will
        // contribute to a message Rating
        int linkLengthValueInChars = 25;

        ProcessedMessage message = new ProcessedMessage(messageRaw);
        int linkContributionToLength = message.getLinkCount() * linkLengthValueInChars;
        int emojiContributionToLength = message.getEmojiCount();
        int length = message.getUniqueLength() + linkContributionToLength + emojiContributionToLength;

        return length * length;
    }


}
