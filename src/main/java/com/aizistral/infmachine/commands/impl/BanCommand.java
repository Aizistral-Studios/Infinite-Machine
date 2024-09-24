package com.aizistral.infmachine.commands.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleDuration;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class BanCommand extends PunishCommand {

    @Override
    protected SlashCommandData setDefaultPermissions(SlashCommandData data, String name) {
        return data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                Permission.BAN_MEMBERS));
    }

    @Override
    protected SlashCommandData addOptionalOptions(SlashCommandData data, String name) {
        return super.addOptionalOptions(data, name)
                .addOption(OptionType.STRING, "clear_time", Lang.get("cmd."+ name +".clear_time.desc"), false);
    }

    @Override
    protected String getCommandName() {
        return "ban";
    }

    @Override
    public Type getActionType() {
        return Type.BAN;
    }

    @Override
    protected boolean checkPreconditions(SlashCommandInteractionEvent event, Context context) {
        if (!super.checkPreconditions(event, context))
            return false;

        String clearStr = event.getOption("clear_time", "0d", OptionMapping::getAsString);

        try {
            SimpleDuration.fromString(clearStr);
        } catch (IllegalArgumentException ex) {
            this.handleValidationError(Lang.get("msg.actionError.invalidDuration", clearStr), event, context);
            return false;
        }

        return true;
    }

    private SimpleDuration applyClearConstraints(SimpleDuration duration) {
        SimpleDuration maxClear = new SimpleDuration(7, TimeUnit.DAYS);

        if (duration.getDuration() < 0)
            return new SimpleDuration(0, TimeUnit.DAYS);
        else if (duration.greaterThan(maxClear))
            return maxClear;

        return duration;
    }

    @Override
    protected void handleSuccess(SlashCommandInteractionEvent event, Context context, User subject, User moderator,
            SimpleDuration duration, ModerationAction action, EmbedBuilder builder, InteractionHook hook) {
        super.handleSuccess(event, context, subject, moderator, duration, action, builder, hook);
        InfiniteDatabase.registerPunishment(action, subject.getIdLong(), context.getGuild().getIdLong());
    }

    @Override
    protected Optional<AuditableRestAction<Void>> getModerationAction(Guild guild, User subject, SimpleDuration duration,
            String reason, SlashCommandInteractionEvent event, Context context) {
        SimpleDuration clear = SimpleDuration.fromString(event.getOption("clear_time", "0d", OptionMapping::getAsString));
        clear = this.applyClearConstraints(clear);

        return Optional.of(guild.ban(subject, (int) clear.getDuration(), clear.getTimeUnit()).reason(reason));
    }

}
