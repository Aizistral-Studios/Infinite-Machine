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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
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
    public Type getActionType() {
        return Type.WARNING;
    }

    @Override
    protected Optional<AuditableRestAction<Void>> getModerationAction(Guild guild, User subject, SimpleDuration duration,
            String reason, SlashCommandInteractionEvent event, Context context) {
        return Optional.empty();
    }

    @Override
    protected EmbedBuilder getReplyTemplate(Context context, User moderator, User subject, SimpleDuration duration,
            String reason) {
        EmbedBuilder builder = super.getReplyTemplate(context, moderator, subject, duration, reason);
        return this.addSpecialFields(builder, subject.getIdLong(), context.getGuild().getIdLong());
    }

    @Override
    protected MessageEmbed getLogEmbed(User moderator, User subject, SimpleDuration duration, String reason,
            SlashCommandInteractionEvent event, Context context) {
        MessageEmbed embed = super.getLogEmbed(moderator, subject, duration, reason, event, context);
        EmbedBuilder builder = new EmbedBuilder(embed);

        return this.addSpecialFields(builder, subject.getIdLong(), context.getGuild().getIdLong()).build();
    }

    private EmbedBuilder addSpecialFields(EmbedBuilder builder, long subjectId, long guildId) {
        int userWarnings = InfiniteDatabase.getUserWarningCount(subjectId, guildId);
        int globalWarnings = InfiniteDatabase.getGlobalWarningCount(guildId);

        builder.addField(Lang.get("msg.warnGlobalCount"), String.valueOf(globalWarnings + 1), true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.warnUserCount"), String.valueOf(userWarnings + 1), true);

        return builder;
    }

    @Override
    protected void handleSuccess(SlashCommandInteractionEvent event, Context context, User subject, User moderator,
            SimpleDuration duration, ModerationAction action, EmbedBuilder builder, InteractionHook hook) {
        super.handleSuccess(event, context, subject, moderator, duration, action, builder, hook);

        int uc = InfiniteDatabase.incrementUserWarningCount(subject.getIdLong(), context.getGuild().getIdLong());
        int gc = InfiniteDatabase.incrementGlobalWarningCount(context.getGuild().getIdLong());

        InfiniteDatabase.registerPunishment(action, subject.getIdLong(), context.getGuild().getIdLong(), gc);
    }

}
