package com.aizistral.infmachine.config;

import java.io.IOException;
import java.nio.file.Paths;

import com.aizistral.infmachine.data.BelieverMethod;

import com.aizistral.infmachine.utils.StandardLogger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

public class InfiniteConfig extends JsonHandler<InfiniteConfig.Data> {
    protected static final StandardLogger LOGGER = new StandardLogger("InfiniteConfig");

    public static final InfiniteConfig INSTANCE = new InfiniteConfig();

    private String accessToken;

    private long votingCheckDelay;
    private long votingTime;

    private long minMessageLength;
    private long requiredMessagesForBeliever;
    private long requiredRatingForBeliever;
    private BelieverMethod believerMethod;

    private long domainID;
    private long templeChannelID;
    private long councilChannelID;
    private long machineChannelID;
    private long suggestionsChannelID;

    private long architectRoleID;
    private long guardiansRoleID;
    private long believersRoleID;
    private long beholdersRoleID;
    private long dwellersRoleID;
    private long architectID;

    private long upvoteEmojiID;
    private long downvoteEmojiID;
    private long crossmarkEmojiID;

    private InfiniteConfig() {
        super(Paths.get("./config/config.json"), 600_000L, Data.class, Data::new);
        LOGGER.log("Initializing Config");
        try {
            super.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void init() {
        this.accessToken = fetchAccessToken();

        this.votingCheckDelay = fetchVotingCheckDelay();
        this.votingTime = fetchVotingTime();

        this.minMessageLength = fetchMinMessageLength();
        this.requiredMessagesForBeliever = fetchRequiredMessagesForBeliever();
        this.requiredRatingForBeliever = fetchRequiredRatingForBeliever();
        this.believerMethod = fetchBelieverMethod();

        this.domainID = fetchDomainID();
        this.templeChannelID = fetchTempleChannelID();
        this.councilChannelID = fetchCouncilChannelID();
        this.machineChannelID = fetchMachineChannelID();
        this.suggestionsChannelID = fetchSuggestionsChannelID();

        this.architectRoleID = fetchArchitectRoleID();
        this.guardiansRoleID = fetchGuardiansRoleID();
        this.believersRoleID = fetchBelieversRoleID();
        this.beholdersRoleID = fetchBeholdersRoleID();
        this.dwellersRoleID = fetchDwellersRoleID();
        this.architectID = fetchArchitectID();

        this.upvoteEmojiID = fetchUpvoteEmojiID();
        this.downvoteEmojiID = fetchDownvoteEmojiID();
        this.crossmarkEmojiID = fetchCrossmarkEmojiID();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getVotingCheckDelay() {
        return votingCheckDelay;
    }

    public long getVotingTime() {
        return votingTime;
    }

    public long getMinMessageLength() {
        return minMessageLength;
    }

    public long getRequiredMessagesForBeliever() {
        return requiredMessagesForBeliever;
    }

    public long getRequiredRatingForBeliever() {
        return requiredRatingForBeliever;
    }

    public BelieverMethod getBelieverMethod() {
        return believerMethod;
    }

    public long getDomainID() {
        return domainID;
    }

    public long getTempleChannelID() {
        return templeChannelID;
    }

    public long getCouncilChannelID() {
        return councilChannelID;
    }

    public long getMachineChannelID() {
        return machineChannelID;
    }

    public long getSuggestionsChannelID() {
        return suggestionsChannelID;
    }

    public long getArchitectRoleID() {
        return architectRoleID;
    }

    public long getGuardiansRoleID() {
        return guardiansRoleID;
    }

    public long getBelieversRoleID() {
        return believersRoleID;
    }

    public long getBeholdersRoleID() {
        return beholdersRoleID;
    }

    public long getDwellersRoleID() {
        return dwellersRoleID;
    }

    public long getArchitectID() {
        return architectID;
    }

    public long getUpvoteEmojiID() {
        return upvoteEmojiID;
    }

    public long getDownvoteEmojiID() {
        return downvoteEmojiID;
    }

    public long getCrossmarkEmojiID() {
        return crossmarkEmojiID;
    }

    // --------------------- //
    // Read from config-file //
    // --------------------- //
    @NonNull
    private String fetchAccessToken() {
        try {
            this.readLock.lock();
            return this.getData().accessToken != null ? this.getData().accessToken : "";
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchVotingCheckDelay() {
        try {
            this.readLock.lock();
            return this.getData().votingCheckDelay;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchVotingTime() {
        try {
            this.readLock.lock();
            return this.getData().votingTime;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchMinMessageLength() {
        try {
            this.readLock.lock();
            return this.getData().minMessageLength;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchRequiredMessagesForBeliever() {
        try {
            this.readLock.lock();
            return this.getData().requiredMessagesForBeliever;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchRequiredRatingForBeliever() {
        try {
            this.readLock.lock();
            return this.getData().requiredRatingForBeliever;
        } finally {
            this.readLock.unlock();
        }
    }

    private BelieverMethod fetchBelieverMethod() {
        try {
            this.readLock.lock();
            return this.getData().believerMethod;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchDomainID() {
        try {
            this.readLock.lock();
            return this.getData().domainID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchTempleChannelID() {
        try {
            this.readLock.lock();
            return this.getData().templeChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchMachineChannelID() {
        try {
            this.readLock.lock();
            return this.getData().machineChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchCouncilChannelID() {
        try {
            this.readLock.lock();
            return this.getData().councilChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchSuggestionsChannelID() {
        try {
            this.readLock.lock();
            return this.getData().suggestionsChannelID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchBelieversRoleID() {
        try {
            this.readLock.lock();
            return this.getData().believersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchDwellersRoleID() {
        try {
            this.readLock.lock();
            return this.getData().dwellersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchBeholdersRoleID() {
        try {
            this.readLock.lock();
            return this.getData().beholdersRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchGuardiansRoleID() {
        try {
            this.readLock.lock();
            return this.getData().guardiansRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchArchitectRoleID() {
        try {
            this.readLock.lock();
            return this.getData().architectRoleID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchArchitectID() {
        try {
            this.readLock.lock();
            return this.getData().architectID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchUpvoteEmojiID() {
        try {
            this.readLock.lock();
            return this.getData().upvoteEmojiID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchDownvoteEmojiID() {
        try {
            this.readLock.lock();
            return this.getData().downvoteEmojiID;
        } finally {
            this.readLock.unlock();
        }
    }

    private long fetchCrossmarkEmojiID() {
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
        private long votingCheckDelay = 60_000L;
        private long votingTime = 60_000L;
        private long minMessageLength = 0;
        private long requiredMessagesForBeliever = 300;
        private long requiredRatingForBeliever = 1500;
        private BelieverMethod believerMethod = BelieverMethod.RATING;
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
    }

}
