package com.aizistral.infmachine.commands.impl;

import java.util.Optional;

import javax.annotation.Nullable;

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
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public abstract class PunishCommand implements Command {

    protected abstract String getCommandName();

    public abstract ModerationAction.Type getActionType();

    protected abstract Optional<AuditableRestAction<Void>> getModerationAction(Guild guild, User subject,
            SimpleDuration duration, String reason, SlashCommandInteractionEvent event, Context context);

    protected String getActionFailDesc(String subjectName) {
        return Lang.get("msg." + this.getCommandName() + "Desc.fail", subjectName);
    }

    protected String getActionSuccessDesc(String subjectName) {
        return Lang.get("msg." + this.getCommandName() + "Desc.success", subjectName);
    }

    protected String getActionLogDesc(String subjectName) {
        return Lang.get("msg." + this.getCommandName() + "Desc.log", subjectName);
    }

    @Override
    public SlashCommandData getData(Context context) {
        String name = this.getCommandName();
        SlashCommandData data = Commands.slash(name, Lang.get("cmd." + name + ".desc"));

        data = this.addRequiredOptions(data, name);
        data = this.addOptionalOptions(data, name);
        data = this.setDefaultPermissions(data, name);

        return data;
    }

    protected SlashCommandData addRequiredOptions(SlashCommandData data, String name) {
        return data.addOption(OptionType.USER, "subject", Lang.get("cmd." + name + ".subject.desc"), true)
                .addOption(OptionType.STRING, "reason", Lang.get("cmd."+ name +".reason.desc"), true);
    }

    protected SlashCommandData addOptionalOptions(SlashCommandData data, String name) {
        return data.addOption(OptionType.STRING, "duration", Lang.get("cmd."+ name +".duration.desc"), false);
    }

    protected SlashCommandData setDefaultPermissions(SlashCommandData data, String name) {
        return data.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    protected String getSubjectDisplayName(SlashCommandInteractionEvent event) {
        Member member = event.getOption("subject", OptionMapping::getAsMember);

        if (member != null)
            return member.getEffectiveName();
        else
            return event.getOption("subject", OptionMapping::getAsUser).getEffectiveName();
    }

    protected String getModeratorDisplayName(SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        if (member != null)
            return member.getEffectiveName();
        else
            return event.getUser().getEffectiveName();
    }

    protected SimpleDuration applyConstraints(SimpleDuration duration) {
        if (duration.getDuration() < 0)
            return duration.withDuration(0);
        else
            return duration;
    }

    protected boolean checkPreconditions(SlashCommandInteractionEvent event, Context context) {
        if (event.getOption("subject", OptionMapping::getAsUser) == null) {
            this.handleValidationError(Lang.get("msg.actionError.noMember"), event, context);
            return false;
        } else if (event.getOption("reason", OptionMapping::getAsString) == null) {
            this.handleValidationError(Lang.get("msg.actionError.noReason"), event, context);
            return false;
        }

        String durationStr = event.getOption("duration", "0d", OptionMapping::getAsString);

        try {
            SimpleDuration.fromString(durationStr);
        } catch (IllegalArgumentException ex) {
            this.handleValidationError(Lang.get("msg.actionError.invalidDuration", durationStr), event, context);
            return false;
        }

        return true;
    }

    private EmbedBuilder getReplyTemplate(User moderator, User subject, SimpleDuration duration, String reason) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.addField(Lang.get("msg.moderationAuthority"), "<@" + moderator.getId() + ">", true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.moderationSubject"), "<@" + subject.getId() + ">", true);
        builder.addField(Lang.get("msg.moderationDuration"), duration.getLocalized(), true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.moderationReason"), reason, true);

        return builder;
    }

    private MessageEmbed getLogEmbed(User moderator, User subject, SimpleDuration duration, String reason,
            SlashCommandInteractionEvent event, Context context) {
        String subjectName = this.getSubjectDisplayName(event);
        String moderatorName = this.getModeratorDisplayName(event);
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(context.getConfig().getJusticeEmoji() + " " + Lang.get("msg.moderationActionTitle"));

        builder.setColor(context.getConfig().getEmbedNormalColor());
        builder.setDescription(this.getActionLogDesc(subjectName));

        moderatorName = "**" + moderatorName + "** (<@" + moderator.getId() + ">)";
        subjectName = "**" + subjectName + "** (<@" + subject.getId() + ">)";

        builder.addField(Lang.get("msg.moderationAuthority"), moderatorName, true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.moderationSubject"), subjectName, true);
        builder.addField(Lang.get("msg.moderationDuration"), duration.getLocalized(), true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.moderationReason"), reason, true);

        return builder.build();
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event, Context context) {
        if (!this.checkPreconditions(event, context))
            return;

        Guild guild = event.getGuild();
        User moderator = event.getUser();
        User subject = event.getOption("subject", OptionMapping::getAsUser);
        String reason = event.getOption("reason", OptionMapping::getAsString);
        String durationStr = event.getOption("duration", "0d", OptionMapping::getAsString);
        SimpleDuration duration = this.applyConstraints(SimpleDuration.fromString(durationStr));
        long timestamp = System.currentTimeMillis();

        EmbedBuilder builder = this.getReplyTemplate(moderator, subject, duration, reason);
        ModerationAction action = new ModerationAction(this.getActionType(), guild.getIdLong(), moderator.getIdLong(),
                subject.getIdLong(), reason, duration.toMillis(), timestamp, false);

        event.deferReply().queue(hook -> {
            try {
                Member memberSubject = event.getOption("subject", OptionMapping::getAsMember);

                if (memberSubject != null) {
                    if (memberSubject.getPermissions().contains(Permission.ADMINISTRATOR)) {
                        this.handleError(event, context, subject, action, builder, hook, new IllegalArgumentException(
                                "Cannot take action against an administrator"));
                        return;
                    }
                }

                var optional = this.getModerationAction(guild, subject, duration, reason, event, context);

                optional.ifPresentOrElse(request -> request.queue(success -> {
                    this.handleSuccess(event, context, subject, moderator, duration, action, builder, hook);
                }, error -> {
                    this.handleError(event, context, subject, action, builder, hook, error);
                }), () -> {
                    this.handleSuccess(event, context, subject, moderator, duration, action, builder, hook);
                });
            } catch (Exception ex) {
                this.handleError(event, context, subject, action, builder, hook, ex);
            }
        });
    }

    protected void handleValidationError(String description, SlashCommandInteractionEvent event, Context context) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(context.getConfig().getEmbedErrorColor());
        builder.setDescription(context.getConfig().getCrossmarkEmoji() + " " + description);

        event.replyEmbeds(builder.build()).queue();
    }

    protected void handleSuccess(SlashCommandInteractionEvent event, Context context, User subject, User moderator,
            SimpleDuration duration, ModerationAction action, EmbedBuilder builder, InteractionHook hook) {
        builder.setColor(context.getConfig().getEmbedNormalColor());
        builder.setDescription(context.getConfig().getCheckmarkEmoji() + " " + this.getActionSuccessDesc(
                this.getSubjectDisplayName(event)));
        action.setSuccess(true);

        hook.sendMessageEmbeds(builder.build()).queue();
        InfiniteDatabase.logAction(action);

        context.getModerationLogChannel().ifPresent(channel -> {
            channel.sendMessageEmbeds(this.getLogEmbed(moderator, subject, duration, action.getReason(), event,
                    context)).queue();
        });
    }

    protected void handleError(SlashCommandInteractionEvent event, Context context, User subject, ModerationAction action,
            EmbedBuilder builder, InteractionHook hook, Throwable error) {
        LOGGER.error("Moderation action failed with exception:", error);
        builder.setColor(context.getConfig().getEmbedErrorColor());
        builder.setDescription(context.getConfig().getCrossmarkEmoji() + " " + this.getActionFailDesc(
                this.getSubjectDisplayName(event)));

        hook.sendMessageEmbeds(builder.build()).queue();
        InfiniteDatabase.logAction(action);
    }

}
