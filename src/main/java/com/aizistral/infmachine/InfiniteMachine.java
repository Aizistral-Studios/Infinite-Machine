package com.aizistral.infmachine;

import com.aizistral.infmachine.utils.SimpleLogger;

import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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

        Bootstrap.JDA.addEventListener(this);
    }

    public String getVersion() {
        String version = Bootstrap.class.getPackage().getImplementationVersion();
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