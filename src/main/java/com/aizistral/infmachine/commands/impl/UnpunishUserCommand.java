package com.aizistral.infmachine.commands.impl;

import java.util.function.Consumer;

import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class UnpunishUserCommand extends UnpunishCommand {

    protected abstract boolean clearPunishment(long userId, long guildId);

    protected abstract String getSuccessReply(User subject);

    protected abstract String getPunishmentNotFoundReply(User subject);

    protected abstract String getErrorReply(User subject);

    protected void handleSuccess(InteractionHook hook, User authority, User subject, Context context) {
        hook.sendMessageEmbeds(this.getReplyEmbed(this.getSuccessReply(subject), true, context)).queue();
        this.logAction(context.getGuild(), authority.getIdLong(), subject.getIdLong(), true);
    }

    protected void handleError(InteractionHook hook, User authority, User subject, Context context,
            boolean punishmentNotFound) {
        String content = punishmentNotFound ? this.getPunishmentNotFoundReply(subject) : this.getErrorReply(subject);
        hook.sendMessageEmbeds(this.getReplyEmbed(content, false, context)).queue();
        this.logAction(context.getGuild(), authority.getIdLong(), subject.getIdLong(), false);
    }

    @Override
    public SlashCommandData getData(Context context) {
        return super.getData(context).addOption(OptionType.USER, "subject",
                Lang.get("cmd." + this.getCommandName() + ".subject.desc"), true);
    }

    protected void withSubject(SlashCommandInteractionEvent event, Context context, Consumer<User> action) {
        User subject = event.getOption("subject").getAsUser();

        if (subject == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(context.getConfig().getEmbedErrorColor());
            builder.setDescription(context.getConfig().getCrossmarkEmoji() + " " + Lang.get("msg.actionError.noMember"));

            event.replyEmbeds(builder.build());
            return;
        }

        action.accept(subject);
    }

}
