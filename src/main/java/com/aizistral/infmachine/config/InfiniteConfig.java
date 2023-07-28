package com.aizistral.infmachine.config;

import java.nio.file.Paths;

import com.aizistral.infmachine.data.IndexationMode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

public class InfiniteConfig extends AsyncJSONConfig<InfiniteConfig.Data> {
    public static final InfiniteConfig INSTANCE = new InfiniteConfig();

    private InfiniteConfig() {
        super(Paths.get("./config/config.json"), 600_000L, Data.class, Data::new);
    }

    @NonNull
    public String getAccessToken() {
        try {
            this.readLock.lock();
            return this.getData().accessToken != null ? this.getData().accessToken : "";
        } finally {
            this.readLock.unlock();
        }
    }

    public IndexationMode getStartupIndexationMode() {
        try {
            this.readLock.lock();
            return this.getData().startupIndexationMode;
        } finally {
            this.readLock.unlock();
        }

    }

    public long getVotingCheckDelay() {
        try {
            this.readLock.lock();
            return this.getData().votingCheckDelay;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getVotingTime() {
        try {
            this.readLock.lock();
            return this.getData().votingTime;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getVotingThreadName() {
        try {
            this.readLock.lock();
            return this.getData().votingThreadName;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingStandard() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingStandard;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingForced() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingForced;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingElapsedPositive() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingElapsedPositive;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingElapsedNegative() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingElapsedNegative;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingOverruledPositive() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingOverruledPositive;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingOverruledNegative() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingOverruledNegative;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingLeft() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingLeft;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgBelieverOnboarding() {
        try {
            this.readLock.lock();
            return this.getData().msgBelieverOnboarding;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgWrongCommandChannel() {
        try {
            this.readLock.lock();
            return this.getData().msgWrongCommandChannel;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgPong() {
        try {
            this.readLock.lock();
            return this.getData().msgPong;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgPongTime() {
        try {
            this.readLock.lock();
            return this.getData().msgPongTime;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgUptime() {
        try {
            this.readLock.lock();
            return this.getData().msgUptime;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgIndexGet() {
        try {
            this.readLock.lock();
            return this.getData().msgIndexGet;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgIndexSetSuccess() {
        try {
            this.readLock.lock();
            return this.getData().msgIndexSetSuccess;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgIndexSetError() {
        try {
            this.readLock.lock();
            return this.getData().msgIndexSetError;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgIndexReset() {
        try {
            this.readLock.lock();
            return this.getData().msgIndexReset;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgTermination() {
        try {
            this.readLock.lock();
            return this.getData().msgTermination;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgLeaderboardHeader() {
        try {
            this.readLock.lock();
            return this.getData().msgLeaderboardHeader;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgLeaderboardHeaderAlt() {
        try {
            this.readLock.lock();
            return this.getData().msgLeaderboardHeaderAlt;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgLeaderboardEntry() {
        try {
            this.readLock.lock();
            return this.getData().msgLeaderboardEntry;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingCmdSuccess() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingCmdSuccess;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgVotingCmdFail() {
        try {
            this.readLock.lock();
            return this.getData().msgVotingCmdFail;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgRatingOwn() {
        try {
            this.readLock.lock();
            return this.getData().msgRatingOwn;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getMsgRating() {
        try {
            this.readLock.lock();
            return this.getData().msgRating;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getDomainID() {
        try {
            this.readLock.lock();
            return this.getData().domainID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getTempleChannelID() {
        try {
            this.readLock.lock();
            return this.getData().templeChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getMachineChannelID() {
        try {
            this.readLock.lock();
            return this.getData().machineChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getCouncilChannelID() {
        try {
            this.readLock.lock();
            return this.getData().councilChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getSuggestionsChannelID() {
        try {
            this.readLock.lock();
            return this.getData().suggestionsChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getBelieversRoleID() {
        try {
            this.readLock.lock();
            return this.getData().believersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getDwellersRoleID() {
        try {
            this.readLock.lock();
            return this.getData().dwellersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getBeholdersRoleID() {
        try {
            this.readLock.lock();
            return this.getData().beholdersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getGuardiansRoleID() {
        try {
            this.readLock.lock();
            return this.getData().guardiansRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getArchitectRoleID() {
        try {
            this.readLock.lock();
            return this.getData().architectRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getArchitectID() {
        try {
            this.readLock.lock();
            return this.getData().architectID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getUpvoteEmojiID() {
        try {
            this.readLock.lock();
            return this.getData().upvoteEmojiID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getDownvoteEmojiID() {
        try {
            this.readLock.lock();
            return this.getData().downvoteEmojiID;
        } finally {
            this.readLock.unlock();
        }
    }

    public long getCrossmarkEmojiID() {
        try {
            this.readLock.lock();
            return this.getData().crossmarkEmojiID;
        } finally {
            this.readLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data {
        private String accessToken = "";
        private IndexationMode startupIndexationMode = IndexationMode.DISABLED;
        private long votingCheckDelay = 60_000L;
        private long votingTime = 60_000L;
        private long domainID = 757941072449241128L;
        private long templeChannelID = 953374742457499659L;
        private long machineChannelID = 1124278424698105940L;
        private long councilChannelID = 1133412399936970802L;
        private long suggestionsChannelID = 986594094270795776L;
        private long believersRoleID = 771377288927117342L;
        private long dwellersRoleID = 964930040359964752L;
        private long beholdersRoleID = 964940092215021678L;
        private long architectRoleID = 757943753779445850L;
        private long guardiansRoleID = 941057860492738590L;
        private long architectID = 545239329656799232L;
        private long upvoteEmojiID = 946944717982142464L;
        private long downvoteEmojiID = 946944748491522098L;
        private long crossmarkEmojiID = 985652668850655322L;
        private String votingThreadName = "Voting on %s (%s)";
        private String msgVotingStandard = "We start the voting for user %s with %s messages!";
        private String msgVotingForced = "The Architect has started the voting for %s!";
        private String msgVotingElapsedPositive = "The voting for %s is over, outcome: positive.";
        private String msgVotingElapsedNegative = "The voting for %s is over, outcome: negative.";
        private String msgVotingOverruledPositive = "The voting for %s was overruled, outcome: positive.";
        private String msgVotingOverruledNegative = "The voting for %s was overruled, outcome: negative.";
        private String msgVotingLeft = "The voting for<@%s> is over, but they left the server.";
        private String msgBelieverOnboarding = "<@%s>, welcome among the <@&771377288927117342>!";
        private String msgWrongCommandChannel = "It is the wrong place and time...";
        private String msgPong = "Pong!";
        private String msgPongTime = "Pong: %d ms";
        private String msgUptime = "The machine has been awake for %1$s hours, %2$s minutes and %3$s seconds.";
        private String msgIndexGet = "Current message indexing mode is: %s";
        private String msgIndexSetSuccess = "Success! Current message indexing mode was set to %s";
        private String msgIndexSetError = "A mistake! No such mode: %s";
        private String msgIndexReset = "Indexation reset successfully!";
        private String msgTermination = "Executing halt-and-catch-fire protocol...";
        private String msgLeaderboardHeader = "**Top 10 Most Active Human-Like Entities:**";
        private String msgLeaderboardHeaderAlt = "**Most Active Human-Like Entities (Positions %s - %s):**";
        private String msgLeaderboardEntry = "%1$s. **%2$s** (<@%3$s>): %4$s messages";
        private String msgVotingCmdSuccess = "Succesfully opened voting for user <@%s>!";
        private String msgVotingCmdFail = "Failed to open a new voting for user <@%s>. Is there one already?";
        private String msgRatingOwn = "Your position on the leaderboard is: **#%s**\nYou have sent %s messages.";
        private String msgRating = "<@%s>'s position on the leaderboard is: **#%s**\nThey have sent %s messages.";
    }

}
