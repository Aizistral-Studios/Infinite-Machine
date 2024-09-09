package com.aizistral.infmachine.commands.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.utils.SimpleDuration;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class WarnCommand extends PunishCommand {

    @Override
    protected SlashCommandData setDefaultPermissions(SlashCommandData data, String name) {
        return data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                Permission.MODERATE_MEMBERS));
    }

    @Override
    protected String getCommandName() {
        return "warn";
    }

    @Override
    protected Optional<AuditableRestAction<Void>> getModerationAction(Guild guild, User subject, SimpleDuration duration,
            String reason, SlashCommandInteractionEvent event, Context context) {
        return Optional.empty();
    }

}
