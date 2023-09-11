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
    }

}
