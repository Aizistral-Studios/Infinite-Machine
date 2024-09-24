package com.aizistral.infmachine.commands.impl;

import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.database.model.ModerationAction;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public abstract class ModerationCommand implements Command {

    public abstract ModerationAction.Type getActionType();

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

}
