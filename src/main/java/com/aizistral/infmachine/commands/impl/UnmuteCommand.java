package com.aizistral.infmachine.commands.impl;

import java.util.function.Consumer;

import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction.Type;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class UnmuteCommand extends UnpunishUserCommand {

    @Override
    public String getCommandName() {
        return "unmute";
    }

    @Override
    public Type getActionType() {
        return Type.REMOVE_MUTE;
    }

    @Override
    public SlashCommandData getData(Context context) {
        return super.getData(context).setDefaultPermissions(DefaultMemberPermissions
                .enabledFor(Permission.MODERATE_MEMBERS));
    }

    @Override
    protected boolean clearPunishment(long userId, long guildId) {
        return InfiniteDatabase.clearMute(userId, guildId);
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event, Context context) {
        this.withSubject(event, context, subject -> {
            User authority = event.getUser();
            boolean recordFound = this.clearPunishment(subject.getIdLong(), context.getGuild().getIdLong());

            event.deferReply().queue(hook -> {
                context.getGuild().retrieveMember(subject).queue(member -> {
                    if (member.isTimedOut()) {
                        member.removeTimeout().reason("Unmuted by " + authority.getAsMention())
                        .queue(success -> {
                            this.handleSuccess(hook, authority, subject, context);
                        }, error -> {
                            this.handleError(hook, authority, subject, context, true);
                        });
                    } else {
                        if (recordFound) {
                            this.handleSuccess(hook, authority, subject, context);
                        } else {
                            this.handleError(hook, authority, subject, context, true);
                        }
                    }
                }, error -> this.handleError(hook, authority, subject, context, false));
            });
        });
    }

    @Override
    protected String getErrorReply(User subject) {
        return String.format("Could not unmute %s.", subject.getAsMention());
    }

    @Override
    protected String getPunishmentNotFoundReply(User subject) {
        return String.format("%s is not currently muted.", subject.getAsMention());
    }

    @Override
    protected String getSuccessReply(User subject) {
        return String.format("%s was unmuted.", subject.getAsMention());
    }

}
