package com.aizistral.infmachine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.data.IndexationMode;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.database.local.JSONDatabase;
import com.aizistral.infmachine.indexation.ExhaustiveMessageIndexer;
import com.aizistral.infmachine.indexation.RealtimeMessageIndexer;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.voting.VotingHandler;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

@Getter
public class InfiniteMachine extends ListenerAdapter {
    protected static final StandardLogger LOGGER = new StandardLogger("InfiniteMachine");
    public static final InfiniteMachine INSTANCE = new InfiniteMachine(Main.JDA);

    static {
        INSTANCE.awake();
    }

    private final JDA jda;
    private final Guild domain;
    private final Role believersRole;
    private final Role beholdersRole;
    private final Role dwellersRole;
    private final TextChannel templeChannel;
    private final TextChannel councilChannel;
    private final TextChannel machineChannel;
    private final TextChannel suggestionsChannel;
    private final MachineDatabase database;
    private final InfiniteConfig config;
    private final long startupTime;

    private IndexationMode indexationMode = IndexationMode.DISABLED;
    private ExhaustiveMessageIndexer exhaustiveIndexer = null;
    private RealtimeMessageIndexer realtimeIndexer = null;
    private VotingHandler votingHandler = null;

    @SneakyThrows
    private InfiniteMachine(JDA jda) {
        this.jda = jda;
        this.config = InfiniteConfig.INSTANCE;
        this.database = JSONDatabase.INSTANCE;
        this.startupTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(this::trySave));

        // TODO Better localization
        this.jda.updateCommands().addCommands(
                Commands.slash("ping", "Ping the machine accross time and space"),
                Commands.slash("version", "Display current version of the machine"),
                Commands.slash("uptime", "Display how long the machine have been awake for"),
                Commands.slash("pet", "Pet the specified creature")
                .addOption(OptionType.USER, "user", "The creature to pet", false),
                Commands.slash("setindexmode", "Change current message indexing mode")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addOption(OptionType.STRING, "mode",
                        "Indexing mode: " + Arrays.stream(IndexationMode.values())
                        .map(IndexationMode::toString).collect(Collectors.joining(", ")), true),
                Commands.slash("getindexmode", "View current message indexing mode")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("terminate", "Halt and catch fire")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("leaderboard", "Show top-10 people in the server by message count")
                .addOption(OptionType.INTEGER, "start", "Start listing people from this position", false),
                Commands.slash("rating", "Show user's position in leaderboard by message count")
                .addOption(OptionType.USER, "user", "User to show the position of", false),
                Commands.slash("clearindex", "Clear all previous message indexation and counts")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("openvoting", "Vote on specified user")
                .addOption(OptionType.USER, "user", "What user is this voting for", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                ).queue();

        this.jda.awaitReady();

        this.domain = jda.getGuildById(this.config.getDomainID());

        if (this.domain == null) {
            this.terminate(new RuntimeException("The Architect's Domain not found"));
            throw new IllegalStateException();
        }

        this.templeChannel = this.domain.getTextChannelById(this.config.getTempleChannelID());
        this.machineChannel = this.domain.getTextChannelById(this.config.getMachineChannelID());
        this.councilChannel = this.domain.getTextChannelById(this.config.getCouncilChannelID());
        this.suggestionsChannel = this.domain.getTextChannelById(this.config.getSuggestionsChannelID());
        this.believersRole = this.domain.getRoleById(this.config.getBelieversRoleID());
        this.dwellersRole = this.domain.getRoleById(this.config.getDwellersRoleID());
        this.beholdersRole = this.domain.getRoleById(this.config.getBeholdersRoleID());
    }

    private void awake() {
        this.votingHandler = new VotingHandler(this.domain, this.councilChannel, this.templeChannel,
                this.believersRole, ImmutableList.of(this.dwellersRole, this.beholdersRole), this.database);

        this.jda.addEventListener(this);
        this.jda.addEventListener(this.votingHandler);

        this.setIndexationMode(this.config.getStartupIndexationMode());

        LOGGER.log("Domain channels: " + this.domain.getChannels().size());

        this.domain.findMembersWithRoles(this.believersRole).onSuccess(list -> {
            list.forEach(member -> {
                int votingCount = this.database.getVotingCount(member.getIdLong());

                if (votingCount <= 0) {
                    LOGGER.log("Set starting voting count of user %s to 1.", member.getEffectiveName());
                    this.database.addVotingCount(member.getIdLong(), 1);
                }
            });
        });

        String version = this.getVersion();

        if (!Objects.equals(this.database.getLastVersion(), version)) {
            this.database.setLastVersion(version);
            this.machineChannel.sendMessage(String.format("<:the_cube:963161249028378735> Version **%s** of"
                    + " Infinite Machine was deployed successfully.", version)).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromGuild() && event.getGuild() == this.domain) {
            if (event.getChannel() == this.suggestionsChannel) {
                if (event.getAuthor().isBot() || event.getAuthor().isSystem())
                    return;

                event.getMessage().addReaction(this.votingHandler.upvote).queue(v -> {
                    event.getMessage().addReaction(this.votingHandler.downvote).queue();
                });
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getChannel().getIdLong() != this.machineChannel.getIdLong() &&
                event.getChannel().getIdLong() != this.templeChannel.getIdLong()) {
            event.reply("It is the wrong place and time...").queue();
            return;
        }

        if ("ping".equals(event.getName())) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(false).flatMap(v -> event.getHook().editOriginalFormat(
                    "Pong: %d ms", System.currentTimeMillis() - time)).queue();
        } else if ("uptime".equals(event.getName())) {
            long uptime = System.currentTimeMillis() - this.startupTime;

            long hrs = TimeUnit.MILLISECONDS.toHours(uptime);
            uptime -= hrs * 60L * 60L * 1000L;
            long mins = TimeUnit.MILLISECONDS.toMinutes(uptime);
            uptime -= mins * 60L * 1000L;
            long secs = TimeUnit.MILLISECONDS.toSeconds(uptime);

            event.reply(String.format(this.config.getMsgUptime(), hrs, mins, secs)).queue();
        } else if ("setindexmode".equals(event.getName())) {
            event.deferReply().queue();

            String modeName = event.getOption("mode").getAsString();
            val mode = Arrays.stream(IndexationMode.values()).filter(m -> m.toString().equals(modeName)).findAny();

            if (mode.isPresent()) {
                this.setIndexationMode(mode.get());
                event.getHook().sendMessage(String.format(this.config.getMsgIndexSetSuccess(), mode.get())).queue();
            } else {
                event.getHook().sendMessage(String.format(this.config.getMsgIndexSetError(), modeName)).queue();
            }
        } else if ("getindexmode".equals(event.getName())) {
            event.reply(String.format(this.config.getMsgIndexGet(), this.getIndexationMode().toString())).queue();
        } else if ("terminate".equals(event.getName())) {
            event.reply(this.config.getMsgTermination()).submit().whenCompleteAsync((a, b) -> this.shutdown());
        } else if ("leaderboard".equals(event.getName())) {
            event.deferReply().flatMap(v -> {
                val option = event.getOption("start");
                int start = option != null ? Math.max(option.getAsInt(), 1) : 1;

                val senders = this.getDatabase().getTopMessageSenders(this.jda, this.domain, start, 10);
                String reply = "";

                if (start == 1) {
                    reply += this.config.getMsgLeaderboardHeader() + "\n";
                } else {
                    reply += String.format(this.config.getMsgLeaderboardHeaderAlt(), start, start + 9) + "\n";
                }

                for (int i = 0; i < senders.size(); i++) {
                    val triple = senders.get(i);
                    reply += String.format("\n" + this.config.getMsgLeaderboardEntry(), i + start, triple.getB(),
                            triple.getA(), triple.getC());
                }

                return event.getHook().sendMessage(reply).setAllowedMentions(Collections.EMPTY_LIST);
            }).queue();
        } else if ("rating".equals(event.getName())) {
            event.deferReply().flatMap(v -> {
                OptionMapping mapping = event.getOption("user");
                User user = null;
                String message = null;

                if (mapping != null && mapping.getAsUser() != event.getUser()) {
                    user = mapping.getAsUser();
                    val rating = this.database.getSenderRating(this.jda, this.domain, user.getIdLong());
                    message = String.format(this.config.getMsgRating(), user.getIdLong(), rating.getA(),
                            rating.getB());
                } else {
                    user = event.getUser();
                    val rating = this.database.getSenderRating(this.jda, this.domain, user.getIdLong());
                    message = String.format(this.config.getMsgRatingOwn(), rating.getA(), rating.getB());
                }

                return event.getHook().sendMessage(message).setAllowedMentions(Collections.EMPTY_LIST);
            }).queue();
        } else if ("clearindex".equals(event.getName())) {
            event.deferReply().flatMap(v -> {
                this.getDatabase().resetIndexation();
                this.getDatabase().forceSave();
                return event.getHook().sendMessage(this.config.getMsgIndexReset());
            }).queue();
        } else if ("openvoting".equals(event.getName())) {
            event.deferReply().setEphemeral(true).queue();
            User user = event.getOption("user").getAsUser();

            this.getDatabase().addMessageCount(user.getIdLong(), 1);
            this.votingHandler.openVoting(user, -1, false).thenAccept(voting -> {
                if (voting == null) {
                    event.getHook().sendMessage(String.format(this.config.getMsgVotingCmdFail(), user.getId()))
                    .queue();
                } else {
                    event.getHook().sendMessage(String.format(this.config.getMsgVotingCmdSuccess(), user.getId()))
                    .queue();
                }
            });
        } else if ("version".equals(event.getName())) {
            event.reply(String.format("The machine's version is: **%s**", this.getVersion())).queue();
        } else if ("pet".equals(event.getName())) {
            OptionMapping mapping = event.getOption("user");
            long id = mapping != null ? mapping.getAsUser().getIdLong() : 310848622642069504L;

            String msg = "<@%s> has been pet.";

            if (id == 1124053065109098708L) { // bot's own ID
                msg = "At the end of times, the <@%s> has pet itself.";
            }

            event.reply(String.format(msg, id)).queue();
        }
    }

    public JDA getJDA() {
        return this.jda;
    }

    public String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public void setIndexationMode(IndexationMode mode) {
        if (this.indexationMode != mode) {
            this.indexationMode = mode;

            if (mode != IndexationMode.EXHAUSTIVE) {
                if (this.exhaustiveIndexer != null && this.exhaustiveIndexer.isActive()) {
                    this.exhaustiveIndexer.halt();
                }
            }

            if (mode != IndexationMode.REALTIME) {
                if (this.realtimeIndexer != null && this.realtimeIndexer.isEnabled()) {
                    this.realtimeIndexer.disable();
                }
            }

            if (mode == IndexationMode.EXHAUSTIVE) {
                if (this.exhaustiveIndexer == null || !this.exhaustiveIndexer.isActive()) {
                    this.exhaustiveIndexer = new ExhaustiveMessageIndexer(this.domain, this.getDatabase());
                    this.exhaustiveIndexer.onConvergence(() -> {
                        this.machineChannel.sendMessage("Achieved convergence in exhaustive indexation mode, "
                                + "switching to real-time...").queue();
                        this.setIndexationMode(IndexationMode.REALTIME);
                    });
                    this.exhaustiveIndexer.activate();
                }
            } else if (mode == IndexationMode.REALTIME) {
                if (this.realtimeIndexer == null) {
                    this.realtimeIndexer = new RealtimeMessageIndexer(this.domain, this.getDatabase());
                    this.jda.addEventListener(this.realtimeIndexer);
                }

                if (!this.realtimeIndexer.isEnabled()) {
                    this.realtimeIndexer.enable();
                }
            }
        }
    }

    public void terminate(Throwable reason) {
        LOGGER.error("Infinite Machine has encountered a fatal error:", reason);
        LOGGER.error("Initiating termination sequence...");
        this.trySave();

        LOGGER.log("Database saved, calling system exit.");
        System.exit(1);
    }

    public void shutdown() {
        LOGGER.log("Infinite Machine is shutting down...");
        this.trySave();

        LOGGER.log("Database saved, calling system exit.");
        System.exit(0);
    }

    private void trySave() {
        LOGGER.log("Trying to save the database...");

        try {
            this.database.forceSave();
        } catch (Throwable ex) {
            LOGGER.error("Failed to save database! Stacktrace:", ex);
        }
    }

}
