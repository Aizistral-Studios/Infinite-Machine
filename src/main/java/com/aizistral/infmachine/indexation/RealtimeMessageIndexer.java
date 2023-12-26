package com.aizistral.infmachine.indexation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.data.Voting;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RealtimeMessageIndexer extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("RealtimeMessageIndexer");

    private final InfiniteConfig config;
    private final MachineDatabase database;
    private final AtomicBoolean enabled;
    private final Guild guild;
    private final List<Role> unvotableRoles;

    public RealtimeMessageIndexer(Guild guild, MachineDatabase database) {
        this.guild = guild;
        this.database = database;
        this.config = InfiniteConfig.INSTANCE;
        this.unvotableRoles = new ArrayList<>();
        this.enabled = new AtomicBoolean(false);

        this.unvotableRoles.add(this.guild.getRoleById(this.config.getArchitectRoleID()));
        this.unvotableRoles.add(this.guild.getRoleById(this.config.getGuardiansRoleID()));
        this.unvotableRoles.add(this.guild.getRoleById(this.config.getBelieversRoleID()));
    }

    public void enable() {
        this.enabled.set(true);
    }

    public void disable() {
        this.enabled.set(false);
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!this.enabled.get())
            return;

        if (event.isFromGuild() && event.getGuild() == this.guild) {
            User user = event.getAuthor();

            if (event.getMessage().getInteraction() != null) {
                user = event.getMessage().getInteraction().getUser();
            }
            if (!user.isBot() && !user.isSystem()) {
                if (user.getIdLong() != Utils.DELETED_USER_ID) {
                    if (event.getMessage().getContentRaw().length() >= this.config.getMinMessageLength()) {
                        this.onNewMessage(user, event.getMessage());
                    }
                }
            }
        }
    }

    private void onNewMessage(User user, Message message) {
        int count = this.database.addMessageCount(user.getIdLong(), 1);
        int rating = this.database.addMessageRating(user.getIdLong(), InfiniteMachine.evaluateMessage(message));
        long requiredMessages = 0;
        long requiredRating = 0;

        switch (this.config.getBelieverMethod()) {
            case MESSAGES:
                requiredMessages = this.config.getRequiredMessagesForBeliever();
                break;
            case RATING:
                requiredRating = this.config.getRequiredRatingForBeliever();
                break;
            case MESSAGES_AND_RATING:
                requiredMessages = this.config.getRequiredMessagesForBeliever();
                requiredRating = this.config.getRequiredRatingForBeliever();
                break;
        }

        if (count >= requiredMessages && rating >= requiredRating) {
            this.guild.retrieveMember(user).queue(member -> {
                for (Role role : member.getRoles()) {
                    if (this.unvotableRoles.contains(role))
                        return;
                }

                // TODO Remove temporary safeguards when VotingHandler handles async better
                if (this.database.getVotingCount(user.getIdLong()) <= 0) {
                    if (this.database.addVotingCount(user.getIdLong(), 1) == 1) {
                        InfiniteMachine.INSTANCE.getVotingHandler().openVoting(user, count, false,
                                Voting.Type.GRANT_ROLE);
                    } else {
                        this.database.addVotingCount(user.getIdLong(), -1);
                    }
                }
            });
        }
    }

}
