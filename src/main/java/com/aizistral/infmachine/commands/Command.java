package com.aizistral.infmachine.commands;

import java.util.List;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.config.GuildConfig;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface Command {

    public SlashCommandData getData(Context context);

    public void onEvent(SlashCommandInteractionEvent event, Context context);

    @FunctionalInterface
    public interface Context {

        public InfiniteMachine getMachine();

        public default Guild getGuild() {
            return this.getMachine().getGuild();
        }

        public default GuildConfig getConfig() {
            return this.getMachine().getConfig();
        }

    }

}
