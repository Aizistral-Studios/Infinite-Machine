package com.aizistral.infmachine.voting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.data.Voting;
import com.aizistral.infmachine.data.VotingStatus;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.utils.StandardLogger;
import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.Emoji.Type;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

// TODO Figure out how to handle async desync
public class VotingHandler extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("VotingHandler");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<MentionType> ONBOARDING_PINGS = ImmutableList.of(MentionType.USER);
    private static final List<MentionType> VOTING_PINGS = ImmutableList.of(MentionType.ROLE);

    private final Guild guild;
    private final InfiniteConfig config;
    private final TextChannel votingChannel;
    private final TextChannel onboardingChannel;
    private final MachineDatabase database;
    public final CustomEmoji upvote;
    public final CustomEmoji downvote;
    public final UnicodeEmoji checkmark;
    public final CustomEmoji crossmark;
    private final List<Role> removeAtOnboarding;
    private final Role believerRole;
    private final Role dwellerRole;
    private final Thread thread;

    public VotingHandler(Guild guild, TextChannel voting, TextChannel onboarding, Role believerRole,
            Role dwellerRole, List<Role> removeAtOnboarding, MachineDatabase database) {
        this.config = InfiniteConfig.INSTANCE;
        this.guild = guild;
        this.votingChannel = voting;
        this.onboardingChannel = onboarding;
        this.database = database;

        this.upvote = guild.retrieveEmojiById(this.config.getUpvoteEmojiID()).complete();
        this.downvote = guild.retrieveEmojiById(this.config.getDownvoteEmojiID()).complete();
        this.crossmark = guild.retrieveEmojiById(this.config.getCrossmarkEmojiID()).complete();
        this.checkmark = Emoji.fromUnicode("U+2705");

        this.believerRole = believerRole;
        this.dwellerRole = dwellerRole;
        this.removeAtOnboarding = removeAtOnboarding;
        this.thread = new Thread(this::coreLoop);
        this.thread.setName("VotingHandler");
        this.thread.start();
    }

    private void coreLoop() {
        LOGGER.log("Starting up voting handler...");

        while (true) {
            LOGGER.debug("Checking open votings...");
            Set<Voting> votings = this.database.getVotings();
            long checkDelay = this.config.getVotingCheckDelay();
            long votingTime = this.config.getVotingTime();

            for (Voting voting : votings) {
                if (System.currentTimeMillis() - voting.getStartingTime() >= votingTime) {
                    LOGGER.log("Closing voting %s...", voting.getMessageID());

                    this.getVotingStatus(voting).thenAcceptAsync(status -> {
                        boolean positive = status.getVotesFor() > status.getVotesAgainst();
                        this.closeVoting(voting, positive, false).join();
                    }).join();
                }
            }

            LOGGER.debug("All votings checked, slumbering.");

            try {
                Thread.sleep(checkDelay);
            } catch (InterruptedException ex) {
                LOGGER.error("Interrupted:", ex);
            }
        }
    }

    private String getName(User user) {
        return String.format("**%s (<@%s>)**", user.getEffectiveName(), user.getId());
    }

    public boolean hasVoting(User user) {
        if (this.database.getVotings().stream().anyMatch(v -> v.getCandidateID() == user.getIdLong()))
            return true;
        else
            return false;
    }

    public CompletableFuture<Voting> openVoting(User user, int msgCount, boolean increment, Voting.Type type) {
        CompletableFuture<Voting> future = new CompletableFuture<>();

        if (this.hasVoting(user)) {
            future.complete(null);
            return future;
        }

        String message = null;

        if (type == Voting.Type.REVOKE_ROLE) {
            message = Localization.translate("msg.revokationVoting", this.getName(user));
        } else if (msgCount < 0) {
            message = Localization.translate("msg.votingForced", this.getName(user));
        } else {
            message = Localization.translate("msg.votingStandard", this.getName(user), msgCount);
        }

        this.votingChannel.sendMessage(message).setAllowedMentions(VOTING_PINGS).queue(msg -> {
            String date = LocalDateTime.now().format(FORMATTER);
            long time = System.currentTimeMillis();

            msg.addReaction(this.upvote).queue(v -> msg.addReaction(this.downvote).queue());
            msg.createThreadChannel(Localization.translate("title.votingThread", user.getName(), date))
            .queue(c -> {
                //c.sendMessage("May your vote be cast in good spirit and with honest intention.").queue();
            });

            Voting voting = new Voting(msg.getIdLong(), user.getIdLong(), time, type);

            this.database.addVoting(voting);

            if (increment) {
                this.database.addVotingCount(user.getIdLong(), 1);
            }

            future.complete(voting);
        });

        return future;
    }

    public CompletableFuture<VotingStatus> getVotingStatus(Voting voting) {
        CompletableFuture<VotingStatus> future = new CompletableFuture<>();

        this.votingChannel.retrieveMessageById(voting.getMessageID()).queue(msg -> {
            MessageReaction upvote = msg.getReaction(this.upvote);
            MessageReaction downvote = msg.getReaction(this.downvote);

            int upvotes = upvote != null ? upvote.getCount() : 0;
            int downvotes = downvote != null ? downvote.getCount() : 0;

            future.complete(new VotingStatus(upvotes, downvotes));
        }, future::completeExceptionally);

        return future;
    }

    public CompletableFuture<Void> closeVoting(Voting voting, boolean positive, boolean overruled) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (this.database.getVotings().contains(voting)) {
            this.votingChannel.retrieveMessageById(voting.getMessageID()).queue(msg -> {
                ThreadChannel thread = msg.getStartedThread();
                GuildMessageChannel channel = thread != null ? thread : this.votingChannel;
                boolean revokation = voting.getType() == Voting.Type.REVOKE_ROLE;
                String votingName = revokation ? "revokationVoting" : "voting";

                this.guild.retrieveMemberById(voting.getCandidateID()).queue(member -> {
                    if (positive) {
                        AuditableRestAction<Void> action = null;

                        if (revokation) {
                            action = this.guild.removeRoleFromMember(member, this.believerRole);
                        } else {
                            action = this.guild.addRoleToMember(member, this.believerRole);
                        }

                        action.queue(v -> {
                            String message = null;

                            if (!overruled) {
                                message = Localization.translate("msg." + votingName + "ElapsedPositive",
                                        this.getName(member.getUser()));
                            } else {
                                message = Localization.translate("msg." + votingName + "OverruledPositive",
                                        this.getName(member.getUser()));
                            }

                            channel.sendMessage(message).setAllowedMentions(ImmutableList.of()).queue(m -> {
                                if (!overruled) {
                                    msg.addReaction(this.checkmark).queue();
                                }

                                if (revokation) {
                                    this.guild.addRoleToMember(member, this.dwellerRole).queue();

                                    member.getUser().openPrivateChannel().queue(dm -> {
                                        dm.sendMessage(Localization.translate("msg.revokationPersonal")).queue();
                                    });
                                } else {
                                    this.removeAtOnboarding.forEach(role -> this.guild.removeRoleFromMember(
                                            member, role).queue());

                                    this.onboardingChannel.sendMessage(Localization.translate(
                                            "msg.believerOnboarding", member.getIdLong())).setAllowedMentions(
                                                    ONBOARDING_PINGS).queue();
                                }

                                this.database.removeVoting(voting);
                                future.complete(null);
                            }, future::completeExceptionally);
                        }, future::completeExceptionally);
                    } else {
                        String message = null;

                        if (!overruled) {
                            message = Localization.translate("msg." + votingName + "ElapsedNegative",
                                    this.getName(member.getUser()));
                        } else {
                            message = Localization.translate("msg." + votingName + "OverruledNegative",
                                    this.getName(member.getUser()));
                        }

                        channel.sendMessage(message).setAllowedMentions(ImmutableList.of()).queue(m -> {
                            if (!overruled) {
                                msg.addReaction(this.crossmark).queue();
                            }

                            this.database.removeVoting(voting);
                            future.complete(null);
                        }, future::completeExceptionally);
                    }

                }, ex -> {
                    this.votingChannel.sendMessage(Localization.translate("msg.votingLeft",
                            voting.getCandidateID()))
                    .queue(m -> {
                        this.database.removeVoting(voting);
                        future.complete(null);
                    }, future::completeExceptionally);
                });
            }, ex -> {
                LOGGER.error("Looks like voting for user %s has disappeared...", voting.getCandidateID());
                LOGGER.error("Silently removing from database with no effects.");

                this.database.removeVoting(voting);
                future.complete(null);
            });
        }

        return future;
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.isFromGuild() && event.getGuild() == this.guild) {
            if (event.getChannel().getIdLong() != this.votingChannel.getIdLong())
                return;

            long msgID = event.getMessageIdLong();
            EmojiUnion emoji = event.getReaction().getEmoji();

            if (emoji.getType() == Type.CUSTOM) {
                long emojiID = emoji.asCustom().getIdLong();

                if (emojiID == this.upvote.getIdLong() || emojiID == this.downvote.getIdLong()) {
                    long userID = event.getUserIdLong();

                    this.database.getVotings().stream().filter(v -> v.getMessageID() == msgID).findAny()
                    .ifPresent(voting -> {
                        if (voting.getCandidateID() == userID && voting.getType() == Voting.Type.REVOKE_ROLE) {
                            event.getReaction().removeReaction(event.getUser()).queue();
                        }
                    });
                }
            }

            if (event.getUserIdLong() != this.config.getArchitectID())
                return;

            AtomicBoolean approve = new AtomicBoolean(false);

            if (emoji.getType() == Type.CUSTOM) {
                if (emoji.asCustom().getIdLong() == this.crossmark.getIdLong()) {
                    approve.set(false);
                } else
                    return;
            } else if (event.getReaction().getEmoji().getType() == Type.UNICODE) {
                if (emoji.asUnicode().getAsCodepoints().equals(this.checkmark.getAsCodepoints())) {
                    approve.set(true);
                } else
                    return;
            }


            this.database.getVotings().stream().filter(v -> v.getMessageID() == msgID).findAny()
            .ifPresent(voting -> {
                this.closeVoting(voting, approve.get(), true);
            });
        }
    }

}
