package com.aizistral.infmachine.commands;

import java.util.List;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface Command {

    public String getName();

    public SlashCommandData getData();

    public void onEvent(SlashCommandInteractionEvent event);

}
