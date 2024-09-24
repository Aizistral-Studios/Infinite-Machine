package com.aizistral.infmachine.commands;

import java.util.List;
import java.util.Optional;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.config.GuildConfig;
import com.aizistral.infmachine.utils.SimpleLogger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface Command {
    public static final SimpleLogger LOGGER = new SimpleLogger("CommandHandler");

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

        public default Optional<StandardGuildMessageChannel> getModerationLogChannel() {
            return this.getMachine().getModerationLogChannel();
        }

    }

}
