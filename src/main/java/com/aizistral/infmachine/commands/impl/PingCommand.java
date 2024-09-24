package com.aizistral.infmachine.commands.impl;

import com.aizistral.infmachine.commands.Command;
import com.aizistral.infmachine.commands.Command.Context;
import com.aizistral.infmachine.config.Lang;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PingCommand implements Command {

    @Override
    public SlashCommandData getData(Context context) {
        return Commands.slash("ping", Lang.get("cmd.ping.desc"));
    }

    @Override
    public void onEvent(SlashCommandInteractionEvent event, Context context) {
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(false).flatMap(v -> event.getHook().editOriginalFormat(
                "Pong: %d ms", System.currentTimeMillis() - time)).queue();
    }

}
