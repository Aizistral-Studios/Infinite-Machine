package com.aizistral.infmachine.commands.impl;

import java.util.Optional;

import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleDuration;
import com.google.common.base.Preconditions;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class MuteCommand implements Command {

    @Override
    public SlashCommandData getData(Context context) {
        return Commands.slash("mute", Localization.get("cmd.mute.desc"))
                .addOption(OptionType.USER, "subject", "The user to be muted", true)
                .addOption(OptionType.STRING, "reason", "Why is this moderation action undertaken?", true)
                .addOption(OptionType.STRING, "duration", "How long the mute should last (default is 3 days)", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));
    }

    private String getSubjectDisplayName(SlashCommandInteractionEvent event) {
        Member member = event.getOption("subject", OptionMapping::getAsMember);

        if (member != null)
            return member.getEffectiveName();
        else
            return  event.getOption("subject", OptionMapping::getAsUser).getEffectiveName();
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event, Context context) {
        Guild guild = event.getGuild();
        User moderator = event.getUser();
        User subject = event.getOption("subject", OptionMapping::getAsUser);
        String reason = event.getOption("reason", OptionMapping::getAsString);
        String durationString = event.getOption("duration", "3d", OptionMapping::getAsString);
        long timestamp = System.currentTimeMillis();

        Preconditions.checkArgument(subject != null && reason != null, "One of required arguments was not supplied");

        SimpleDuration duration;

        try {
            duration = SimpleDuration.fromString(durationString);
        } catch (IllegalArgumentException ex) {
            EmbedBuilder builder = new EmbedBuilder();

            builder.setColor(context.getConfig().getEmbedErrorColor());
            builder.setDescription(Localization.get("msg.muteDesc.invalidDuration", durationString));

            event.replyEmbeds(builder.build()).queue();
            return;
        }

        ModerationAction action = new ModerationAction(Type.MUTE, guild.getIdLong(), moderator.getIdLong(),
                subject.getIdLong(), reason, duration.toMillis(), timestamp, false);
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(context.getConfig().getEmbedErrorColor())
        .setDescription(Localization.get("msg.muteDesc.fail", this.getSubjectDisplayName(event)))
        .addField(Localization.get("msg.moderationAuthority"), "<@" + moderator.getId() + ">", true)
        .addBlankField(true)
        .addField(Localization.get("msg.moderationSubject"), "<@" + subject.getId() + ">", true)
        .addField(Localization.get("msg.moderationDuration"), duration.getLocalized(), true)
        .addBlankField(true)
        .addField(Localization.get("msg.moderationReason"), reason, true);

        event.deferReply().queue(hook -> {
            try {
                event.getGuild().timeoutFor(subject, duration.getDuration(), duration.getTimeUnit()).queue(success -> {
                    builder.setColor(context.getConfig().getEmbedNormalColor());
                    builder.setDescription(Localization.get("msg.muteDesc.success", this.getSubjectDisplayName(event)));
                    action.setSuccess(true);

                    hook.sendMessageEmbeds(builder.build()).queue();
                    InfiniteDatabase.logAction(action);
                }, error -> {
                    LOGGER.error("Mute command failed with exception:", error);

                    hook.sendMessageEmbeds(builder.build()).queue();
                    InfiniteDatabase.logAction(action);
                });
            } catch (Exception ex) {
                LOGGER.error("Mute command failed with abnormal exception:", ex);

                hook.sendMessageEmbeds(builder.build()).queue();
                InfiniteDatabase.logAction(action);
            }
        });
    }

}
