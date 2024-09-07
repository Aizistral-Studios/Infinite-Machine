package com.aizistral.infmachine.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aizistral.infmachine.Bootstrap;
import com.aizistral.infmachine.commands.impl.KillCommand;
import com.aizistral.infmachine.commands.impl.PingCommand;
import com.aizistral.infmachine.config.Localization;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class CommandRegistry implements EventListener {
    @Getter
    private static CommandRegistry instance;
    private final Map<String, Command> commands = new HashMap<>();

    private CommandRegistry() {
        // NO-OP
    }

    private void populate() {
        this.register(new PingCommand());
        this.register(new KillCommand());
    }

    private void register(Command command) {
        this.commands.put(command.getData().getName(), command);
    }

    private void sendUpdate() {
        Bootstrap.JDA.updateCommands().addCommands(this.commands.values().stream().map(Command::getData).toList()).queue();
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            Command command = this.commands.get(slashEvent.getName());

            if (command != null) {
                command.onEvent(slashEvent);
            } else {
                slashEvent.reply(Localization.get("msg.commandNotFound")).queue();
            }
        }
    }

    public static void bootstrap() {
        Preconditions.checkArgument(instance == null, "CommandRegistry already bootstrapped!");

        instance = new CommandRegistry();
        instance.populate();
        instance.sendUpdate();

        Bootstrap.JDA.addEventListener(instance);
    }

}
