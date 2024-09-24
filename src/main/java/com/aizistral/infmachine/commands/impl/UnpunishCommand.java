package com.aizistral.infmachine.commands.impl;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class UnpunishCommand extends ModerationCommand {

    public abstract String getCommandName();

    protected MessageEmbed getReplyEmbed(String content, boolean success, Context context) {
        EmbedBuilder builder = new EmbedBuilder();

        if (success) {
            builder.setColor(context.getConfig().getEmbedNormalColor());
            builder.setDescription(context.getConfig().getCheckmarkEmoji() + " " + content);
        } else {
            builder.setColor(context.getConfig().getEmbedErrorColor());
            builder.setDescription(context.getConfig().getCrossmarkEmoji() + " " + content);
        }

        return builder.build();
    }

    protected MessageEmbed getLogEmbed(String content, String authorityName, String subjectName, Context context) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(context.getConfig().getEmbedNormalColor());
        builder.setTitle(context.getConfig().getUnpunishedEmoji() + " " + Lang.get("msg."
                + this.getCommandName() + ".logTitle"));
        builder.setDescription(content);

        builder.addField(Lang.get("msg.moderationAuthority"), authorityName, true);
        builder.addBlankField(true);
        builder.addField(Lang.get("msg.moderationSubject"), subjectName, true);

        return builder.build();
    }

    protected void logAction(Guild guild, long authorityId, long subjectId, boolean success) {
        InfiniteDatabase.logAction(new ModerationAction(this.getActionType(), guild.getIdLong(), authorityId,
                subjectId, "N/A", -1, System.currentTimeMillis(), success));
    }

    @Override
    public SlashCommandData getData(Context context) {
        return Commands.slash(this.getCommandName(), Lang.get("cmd." + this.getCommandName() + ".desc"));
    }

}
