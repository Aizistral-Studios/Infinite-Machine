package com.aizistral.infmachine.config;

import java.io.IOException;
import java.nio.file.Paths;

import com.aizistral.infmachine.data.BelieverMethod;

import com.aizistral.infmachine.data.ExitCode;
import com.aizistral.infmachine.utils.StandardLogger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class InfiniteConfig extends JsonHandler<InfiniteConfig.Data> {
    protected static final StandardLogger LOGGER = new StandardLogger("InfiniteConfig");

    public static final InfiniteConfig INSTANCE = new InfiniteConfig();

    private JDA jda;

    private String accessToken;
    private long votingTime;

    private long minMessageLength;
    private long requiredMessagesForBeliever;
    private long requiredRatingForBeliever;
    private BelieverMethod believerMethod;

    private Guild domain;
    private TextChannel templeChannel;
    private TextChannel councilChannel;
    private TextChannel machineChannel;
    private TextChannel suggestionsChannel;

    private Role architectRole;
    private Role petmasterRole;
    private Role guardiansRole;
    private Role believersRole;
    private Role beholdersRole;
    private Role dwellersRole;
    private Role cursedRole;
    private User architect;

    private Emoji upvoteEmoji;
    private Emoji downvoteEmoji;
    private Emoji crossmarkEmoji;

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
    }

    public void load(JDA jda) {
        this.jda = jda;
        this.votingTime = fetchVotingTime();

        this.minMessageLength = fetchMinMessageLength();
        this.requiredMessagesForBeliever = fetchRequiredMessagesForBeliever();
        this.requiredRatingForBeliever = fetchRequiredRatingForBeliever();
        this.believerMethod = fetchBelieverMethod();

        this.domain = jda.getGuildById(fetchDomainID());
        if (this.domain == null) {
            LOGGER.error("Architects Domain could not be located. Is the machine not there yet?");
            System.exit(ExitCode.MISSING_DOMAIN_ERROR.getCode());
        }

        this.templeChannel = domain.getTextChannelById(fetchTempleChannelID());
        this.councilChannel = domain.getTextChannelById(fetchCouncilChannelID());
        this.machineChannel = domain.getTextChannelById(fetchMachineChannelID());
        this.suggestionsChannel = domain.getTextChannelById(fetchSuggestionsChannelID());

        this.architectRole = domain.getRoleById(fetchArchitectRoleID());
        this.petmasterRole = domain.getRoleById(fetchPetmasterRoleID());
        this.guardiansRole = domain.getRoleById(fetchGuardiansRoleID());
        this.believersRole = domain.getRoleById(fetchBelieversRoleID());
        this.beholdersRole = domain.getRoleById(fetchBeholdersRoleID());
        this.dwellersRole = domain.getRoleById(fetchDwellersRoleID());
        this.cursedRole = domain.getRoleById((fetchCursedRoleID()));
        this.architect = jda.retrieveUserById(fetchArchitectID()).complete();

        this.upvoteEmoji = domain.getEmojiById(fetchUpvoteEmojiID());
        this.downvoteEmoji = domain.getEmojiById(fetchDownvoteEmojiID());
        this.crossmarkEmoji = domain.getEmojiById(fetchCrossmarkEmojiID());
        forceSave();
    }



    public JDA getJDA() {
        return jda;
    }

    public String getAccessToken() {
        return accessToken;
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

    public Guild getDomain() {
        return domain;
    }

    public TextChannel getTempleChannel() {
        return templeChannel;
    }

    public TextChannel getCouncilChannel() {
        return councilChannel;
    }

    public TextChannel getMachineChannel() {
        return machineChannel;
    }

    public TextChannel getSuggestionsChannel() {
        return suggestionsChannel;
    }

    public Role getArchitectRole() {
        return architectRole;
    }

    public Role getPetmasterRole() {
        return petmasterRole;
    }

    public Role getGuardiansRole() {
        return guardiansRole;
    }

    public Role getBelieversRole() {
        return believersRole;
    }

    public Role getBeholdersRole() {
        return beholdersRole;
    }

    public Role getDwellersRole() {
        return dwellersRole;
    }

    public Role getCursedRole() {
        return cursedRole;
    }

    public User getArchitect() {
        return architect;
    }

    public Emoji getUpvoteEmoji() {
        return upvoteEmoji;
    }

    public Emoji getDownvoteEmoji() {
        return downvoteEmoji;
    }

    public Emoji getCrossmarkEmoji() {
        return crossmarkEmoji;
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

    private long fetchVotingTime() {
        try {
            this.readLock.lock();
            return this.getData().votingTimeInHours;
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

    private long fetchCursedRoleID() {
        try {
            this.readLock.lock();
            return this.getData().cursedRoleID;
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

    private long fetchPetmasterRoleID() {
        try {
            this.readLock.lock();
            return this.getData().petmasterRoleID;
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
        private long votingTimeInHours = 48;
        private long minMessageLength = 0;
        private long requiredMessagesForBeliever = 300;
        private long requiredRatingForBeliever = 3750000;
        private BelieverMethod believerMethod = BelieverMethod.RATING;
        private long domainID = 757941072449241128L;
        private long templeChannelID = 953374742457499659L;
        private long machineChannelID = 1124278424698105940L;
        private long councilChannelID = 1133412399936970802L;
        private long suggestionsChannelID = 986594094270795776L;
        private long believersRoleID = 771377288927117342L;
        private long cursedRoleID = 1157706494695985275L;
        private long dwellersRoleID = 964930040359964752L;
        private long beholdersRoleID = 964940092215021678L;
        private long architectRoleID = 757943753779445850L;
        private long guardiansRoleID = 941057860492738590L;
        private long petmasterRoleID = 1215676063716474890L;
        private long architectID = 545239329656799232L;
        private long upvoteEmojiID = 946944717982142464L;
        private long downvoteEmojiID = 946944748491522098L;
        private long crossmarkEmojiID = 985652668850655322L;
    }

}
