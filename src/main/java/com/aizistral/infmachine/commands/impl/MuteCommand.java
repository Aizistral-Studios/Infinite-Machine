package com.aizistral.infmachine.commands.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleDuration;
import com.google.common.base.Preconditions;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class MuteCommand extends PunishCommand {

    @Override
    protected SlashCommandData setDefaultPermissions(SlashCommandData data, String name) {
        return data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                Permission.MODERATE_MEMBERS));
    }

    @Override
    protected String getCommandName() {
        return "mute";
    }

    @Override
    public Type getActionType() {
        return Type.MUTE;
    }

    @Override
    protected SimpleDuration applyConstraints(SimpleDuration duration) {
        SimpleDuration maxDuration = new SimpleDuration(27, TimeUnit.DAYS);
        duration = super.applyConstraints(duration);

        if (duration.greaterThan(maxDuration))
            return maxDuration;
        else if (duration.getDuration() <= 0)
            return new SimpleDuration(3, TimeUnit.DAYS);

        return duration;
    }

    @Override
    protected Optional<AuditableRestAction<Void>> getModerationAction(Guild guild, User subject, SimpleDuration duration,
            String reason, SlashCommandInteractionEvent event, Context context) {
        return Optional.of(guild.timeoutFor(subject, duration.getDuration(), duration.getTimeUnit()).reason(reason));
    }

}
