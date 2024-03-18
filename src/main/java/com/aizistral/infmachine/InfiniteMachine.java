package com.aizistral.infmachine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.data.IndexationMode;
import com.aizistral.infmachine.data.LeaderboardOrder;
import com.aizistral.infmachine.data.ProcessedMessage;
import com.aizistral.infmachine.data.Voting;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.database.local.JSONDatabase;
import com.aizistral.infmachine.indexation.OldExhaustiveMessageIndexer;
import com.aizistral.infmachine.indexation.OldRealtimeMessageIndexer;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.voting.VotingHandler;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
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
    private final long minMessageLength;
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
    private OldExhaustiveMessageIndexer exhaustiveIndexer = null;
    private OldRealtimeMessageIndexer realtimeIndexer = null;
    private VotingHandler votingHandler = null;

    @SneakyThrows
    private InfiniteMachine(JDA jda) {
        this.jda = jda;
        this.config = InfiniteConfig.INSTANCE;
        this.database = JSONDatabase.INSTANCE;
        this.startupTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(this::trySave));

        registerCommands();

        this.jda.awaitReady();

        this.minMessageLength = this.config.getMinMessageLength();
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

    private void registerCommands() {
        // TODO Better localization
        this.jda.updateCommands().addCommands(
                Commands.slash("ping", Localization.translate("cmd.ping.desc")),
                Commands.slash("version", Localization.translate("cmd.version.desc")),
                Commands.slash("uptime", Localization.translate("cmd.uptime.desc")),
                Commands.slash("pet", Localization.translate("cmd.pet.desc"))
                .addOption(OptionType.USER, "user", Localization.translate("cmd.pet.user"), false),
                Commands.slash("sendmessage", Localization.translate("cmd.sendmessage.desc"))
                        .addOption(OptionType.CHANNEL, "channel", Localization.translate("cmd.sendmessage.channel"), true)
                        .addOption(OptionType.STRING, "message", Localization.translate("cmd.sendmessage.message"), true)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("setindexmode", Localization.translate("cmd.setindexmode.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addOption(OptionType.STRING, "mode", Localization.translate("cmd.setindexmode.mode",
                        Arrays.stream(IndexationMode.values()).map(IndexationMode::toString)
                        .collect(Collectors.joining("/"))), true),
                Commands.slash("getindexmode", Localization.translate("cmd.getindexmode.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("terminate", Localization.translate("cmd.terminate.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("leaderboard", Localization.translate("cmd.leaderboard.desc"))
                .addOption(OptionType.STRING, "order", Localization.translate("cmd.leaderboard.order",
                        Arrays.stream(LeaderboardOrder.values()).map(LeaderboardOrder::toString)
                        .collect(Collectors.joining("/"))), false)
                .addOption(OptionType.INTEGER, "start", Localization.translate("cmd.leaderboard.start"), false),
                Commands.slash("rating", Localization.translate("cmd.rating.desc"))
                .addOption(OptionType.USER, "user", Localization.translate("cmd.rating.user"), false),
                Commands.slash("clearindex", Localization.translate("cmd.clearindex.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("openvoting", Localization.translate("cmd.openvoting.desc"))
                .addOption(OptionType.USER, "user", Localization.translate("cmd.openvoting.user"), true)
                .addOption(OptionType.STRING, "type",Localization.translate("cmd.openvoting.type",
                        Arrays.stream(Voting.Type.values()).map(Voting.Type::toString)
                        .collect(Collectors.joining("/"))), false)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                ).queue();
    }

    private void awake() {
        this.votingHandler = new VotingHandler(this.domain, this.councilChannel, this.templeChannel,
                this.believersRole, this.dwellersRole, ImmutableList.of(this.dwellersRole, this.beholdersRole),
                this.database);

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
                event.getChannel().getIdLong() != this.templeChannel.getIdLong() &&
                !"pet".equals(event.getName())) {
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

            event.reply(Localization.translate("msg.uptime", hrs, mins, secs)).queue();
        } else if ("setindexmode".equals(event.getName())) {
            event.deferReply().queue();

            String modeName = event.getOption("mode").getAsString();
            val mode = Arrays.stream(IndexationMode.values()).filter(m -> m.toString().equals(modeName)).findAny();

            if (mode.isPresent()) {
                this.setIndexationMode(mode.get());
                event.getHook().sendMessage(Localization.translate("msg.indexSetSuccess", mode.get())).queue();
            } else {
                event.getHook().sendMessage(Localization.translate("msg.indexSetError", modeName)).queue();
            }
        } else if ("getindexmode".equals(event.getName())) {
            event.reply(Localization.translate("msg.indexGet", this.getIndexationMode().toString())).queue();
        } else if ("terminate".equals(event.getName())) {
            event.reply(Localization.translate("msg.termination")).submit().whenCompleteAsync(
                    (a, b) -> this.shutdown());
        } else if ("leaderboard".equals(event.getName())) {
            event.deferReply().flatMap(v -> {
                OptionMapping orderMapping = event.getOption("order");
                String orderName = orderMapping != null ? orderMapping.getAsString() : "RATING";
                val orderVal = Arrays.stream(LeaderboardOrder.values()).filter(m -> m.toString().equals(orderName)).findAny();

                //Initializing type with default value
                LeaderboardOrder order = LeaderboardOrder.RATING;

                if (orderVal.isPresent()) {
                    order = orderVal.get();
                }

                if (order == LeaderboardOrder.MESSAGES) {
                    val option = event.getOption("start");
                    int start = option != null ? Math.max(option.getAsInt(), 1) : 1;

                    val senders = this.getDatabase().getTopMessageSenders(this.jda, this.domain, order, start, 10);
                    String reply = "";

                    if (start == 1) {
                        reply += Localization.translate("msg.leaderboardHeader") + "\n";
                    } else {
                        reply += Localization.translate("msg.leaderboardHeaderAlt", start, start + 9) + "\n";
                    }

                    for (int i = 0; i < senders.size(); i++) {
                        val leaderboardEntry = senders.get(i);
                        reply += "\n" + Localization.translate("msg.leaderboardEntryMessages", i + start,
                                leaderboardEntry.getUserName(), leaderboardEntry.getUserID(),
                                leaderboardEntry.getMessageCountFormatted(),
                                leaderboardEntry.getDisplayRatingFormatted());
                    }

                    return event.getHook().sendMessage(reply).setAllowedMentions(Collections.EMPTY_LIST);
                } else if (order == LeaderboardOrder.RATING) {
                    val option = event.getOption("start");
                    int start = option != null ? Math.max(option.getAsInt(), 1) : 1;

                    val senders = this.getDatabase().getTopMessageSenders(this.jda, this.domain, order, start, 10);
                    String reply = "";

                    if (start == 1) {
                        reply += Localization.translate("msg.leaderboardHeader") + "\n";
                    } else {
                        reply += Localization.translate("msg.leaderboardHeaderAlt", start, start + 9) + "\n";
                    }

                    for (int i = 0; i < senders.size(); i++) {
                        val leaderboardEntry = senders.get(i);
                        reply += "\n" + Localization.translate("msg.leaderboardEntryRating", i + start,
                                leaderboardEntry.getUserName(), leaderboardEntry.getUserID(),
                                leaderboardEntry.getDisplayRatingFormatted(),
                                leaderboardEntry.getMessageCountFormatted());
                    }

                    return event.getHook().sendMessage(reply).setAllowedMentions(Collections.EMPTY_LIST);
                }

                String reply = "Unrecognized Leaderboard Type";
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
                    message = Localization.translate("msg.rating", user.getIdLong(), rating.getPositionByRating(),
                            rating.getDisplayRatingFormatted(), rating.getPositionByMessages(),
                            rating.getMessageCountFormatted());
                } else {
                    user = event.getUser();
                    val rating = this.database.getSenderRating(this.jda, this.domain, user.getIdLong());
                    message = Localization.translate("msg.ratingOwn", rating.getPositionByRating(),
                            rating.getDisplayRatingFormatted(), rating.getPositionByMessages(),
                            rating.getMessageCountFormatted());
                }

                return event.getHook().sendMessage(message).setAllowedMentions(Collections.EMPTY_LIST);
            }).queue();
        } else if ("clearindex".equals(event.getName())) {
            event.deferReply().flatMap(v -> {
                this.getDatabase().resetIndexation();
                this.getDatabase().forceSave();
                return event.getHook().sendMessage(Localization.translate("msg.indexReset"));
            }).queue();
        } else if ("openvoting".equals(event.getName())) {
            event.deferReply().setEphemeral(true).queue();

            User user = event.getOption("user").getAsUser();
            OptionMapping typeMapping = event.getOption("type");
            Voting.Type type = typeMapping != null ? Voting.Type.valueOf(typeMapping.getAsString())
                    : Voting.Type.GRANT_ROLE;

            this.getDatabase().addMessageCount(user.getIdLong(), 1);
            this.votingHandler.openVoting(user, -1, false, type).thenAccept(voting -> {
                if (voting == null) {
                    event.getHook().sendMessage(Localization.translate("msg.openVotingFail", user.getId()))
                    .queue();
                } else {
                    event.getHook().sendMessage(Localization.translate("msg.openVotingSuccess", user.getId()))
                    .queue();
                }
            });
        } else if ("version".equals(event.getName())) {
            event.reply(String.format("The machine's version is: **%s**", this.getVersion())).queue();
        } else if ("pet".equals(event.getName())) {
            OptionMapping mapping = event.getOption("user");
            long id = mapping != null ? mapping.getAsUser().getIdLong() : 310848622642069504L;

            String msg = "<@%s> has been pet <a:pat_pat_pat:1211592019680694272>";

            //TODO add more funny interactions
            if (id == 440381346339094539L) {
                // Added custom bypass of arkadys anti petting code (feel free to remove if you don't agree)
                if (event.getUser().getIdLong() == 267067816627273730L) {
                    msg = String.format("<@%s> has been pet <a:pat_pat_pat:1211592019680694272>\nWait how did you do that?", id);
                } else {
                    msg = "You should know, that a soul can't be `/pet`\n(CAN'T BE `/PET`!)\n"
                            + "No matter what machines you wield...";
                }
                event.reply(msg).queue();
                return;
            } else if (id == 1124053065109098708L) { // bot's own ID
                msg = "At the end of times, the <@%s> has pet itself <a:pat_pat_pat:1211592019680694272>";
            } else if (event.getUser().getIdLong() == 267067816627273730L) {
                msg = "<@%s> has been masterfully pet";
            }
            event.reply(String.format(msg, id)).queue();
        } else if ("sendmessage".equals(event.getName())){
            OptionMapping channelMapping = event.getOption("channel");
            OptionMapping messageMapping = event.getOption("message");

            long channelID = channelMapping.getAsChannel().getIdLong();
            String message = messageMapping.getAsString();

            Channel targetChannel = jda.getGuildChannelById(channelID);
            if(!(targetChannel instanceof TextChannel)){
                event.reply("The specified channel is not a valid text channel.").queue();
            } else {
                ((TextChannel) targetChannel).sendMessage(message).queue(v -> {
                    event.reply("Message as been send.").queue();
                });
            }
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
                    this.exhaustiveIndexer = new OldExhaustiveMessageIndexer(this.domain, this.getDatabase(), this.minMessageLength);
                    this.exhaustiveIndexer.onConvergence(() -> {
                        this.machineChannel.sendMessage("Achieved convergence in exhaustive indexation mode, "
                                + "switching to real-time...").queue();
                        this.setIndexationMode(IndexationMode.REALTIME);
                    });
                    this.exhaustiveIndexer.activate();
                }
            } else if (mode == IndexationMode.REALTIME) {
                if (this.realtimeIndexer == null) {
                    this.realtimeIndexer = new OldRealtimeMessageIndexer(this.domain, this.getDatabase());
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

    public static int getDispayRating(int points) {
        int segmentLength = 50;
        return points / (segmentLength * segmentLength);
    }

}
