package com.aizistral.infmachine.commands.impl;

import com.aizistral.infmachine.MachineBootstrap;
import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ModerationAction.Type;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class UnwarnCommand extends UnpunishCommand {

    @Override
    public String getCommandName() {
        return "unwarn";
    }

    @Override
    public Type getActionType() {
        return Type.REMOVE_WARNING;
    }

    @Override
    public SlashCommandData getData(Context context) {
        OptionData caseId = new OptionData(OptionType.INTEGER, "case_id", Lang.get("cmd.unwarn.case_id.desc"))
                .setRequired(true).setRequiredRange(0, 1_048_576);
        return super.getData(context).addOptions(caseId)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event, Context context) {
        int caseId = event.getOption("case_id").getAsInt();

        event.deferReply().queue(hook -> {
            InfiniteDatabase.clearWarning(caseId, context.getGuild().getIdLong()).ifPresentOrElse(action -> {
                String desc = String.format("Warning **#%s** removed from <@%s>.", caseId, action.getSubjectId());
                hook.sendMessageEmbeds(this.getReplyEmbed(desc, true, context)).queue();
                this.logAction(event.getGuild(), event.getUser().getIdLong(), action.getSubjectId(), true);

                context.getModerationLogChannel().ifPresent(channel -> {
                    String authority = this.getModeratorDisplayName(event) + " (" + event.getUser().getAsMention() + ")";
                    String contentKey = "msg.unwarn.logContent";

                    context.getGuild().retrieveMemberById(action.getSubjectId()).queue(member -> {
                        String subject = member.getEffectiveName() + " (" + member.getAsMention() + ")";
                        String content = Lang.get(contentKey, caseId, member.getEffectiveName());

                        channel.sendMessageEmbeds(this.getLogEmbed(content, authority, subject, context)).queue();
                    }, error -> {
                        MachineBootstrap.getJDA().retrieveUserById(action.getSubjectId()).queue(user -> {
                            String subject = user.getEffectiveName() + " (" + user.getAsMention() + ")";
                            String content = Lang.get(contentKey, caseId, user.getEffectiveName());

                            channel.sendMessageEmbeds(this.getLogEmbed(content, authority, subject, context)).queue();
                        }, moreError -> {
                            String subject = "UNKNOWN (<@" + action.getSubjectId() + ">)";
                            String content = Lang.get(contentKey, caseId, "UNKNOWN");

                            channel.sendMessageEmbeds(this.getLogEmbed(content, authority, subject, context)).queue();
                        });
                    });
                });
            }, () -> {
                String desc = String.format("No such warning: **#%s**", caseId);
                hook.sendMessageEmbeds(this.getReplyEmbed(desc, false, context)).queue();
                this.logAction(event.getGuild(), event.getUser().getIdLong(), -1, false);
            });
        });
    }

}
