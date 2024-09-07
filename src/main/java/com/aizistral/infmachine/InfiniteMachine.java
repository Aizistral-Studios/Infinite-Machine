package com.aizistral.infmachine;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.aizistral.infmachine.utils.SimpleLogger;

import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder;
import net.dv8tion.jda.api.utils.messages.MessagePollData;

@Getter
public class InfiniteMachine extends ListenerAdapter {
    protected static final SimpleLogger LOGGER = new SimpleLogger("InfiniteMachine");
    public static final InfiniteMachine INSTANCE = new InfiniteMachine();

    @Getter
    private final long startupTime;
    @Getter
    private final Guild domain;

    @SneakyThrows
    private InfiniteMachine() {
        this.startupTime = System.currentTimeMillis();
        this.domain = null;

        Main.JDA.addEventListener(this);
    }

    public String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(false).flatMap(v -> event.getHook().editOriginalFormat(
                    "Pong: %d ms", System.currentTimeMillis() - time)).queue();
        } else {
            event.reply("I don't know any such command...").queue();
        }
    }

    public void terminate() {
        this.terminate(0);
    }

    public void terminate(int code) {
        LOGGER.info("Infinite Machine is shutting down...");
        System.exit(code);
    }

}