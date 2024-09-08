package com.aizistral.infmachine.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.MachineBootstrap;
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
    private final InfiniteMachine machine;
    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry(InfiniteMachine machine) {
        this.machine = machine;
    }

    private void populate() {
        this.register(new PingCommand());

        if (!this.machine.getConfig().isTrusted())
            return;

        this.register(new KillCommand());
    }

    private void register(Command command) {
        this.commands.put(command.getData(this::getMachine).getName(), command);
    }

    private void sendUpdate() {
        var commands = this.commands.values().stream().map(c -> c.getData(this::getMachine)).toList();
        this.machine.getGuild().updateCommands().addCommands(commands).queue();
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (!this.machine.represents(slashEvent.getGuild()))
                return;

            Command command = this.commands.get(slashEvent.getName());

            if (command != null) {
                command.onEvent(slashEvent, this::getMachine);
            } else {
                slashEvent.reply(Localization.get("msg.commandNotFound")).queue();
            }
        }
    }

    public void initialize() {
        Preconditions.checkArgument(this.commands.isEmpty(), "CommandRegistry already initialized!");

        this.populate();
        this.machine.getJDA().addEventListener(this);
        this.sendUpdate();
    }

}
