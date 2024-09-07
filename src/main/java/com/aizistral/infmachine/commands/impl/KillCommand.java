package com.aizistral.infmachine.commands.impl;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.config.Localization;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class KillCommand implements Command {

    @Override
    public SlashCommandData getData() {
        return Commands.slash("terminate", Localization.get("cmd.terminate.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event) {
        event.reply(Localization.get("msg.termination")).queue(hook -> {
            InfiniteMachine.INSTANCE.terminate();
        });
    }

}
