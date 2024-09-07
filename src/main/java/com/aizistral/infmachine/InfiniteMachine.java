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
    }

    public String getVersion() {
        String version = Bootstrap.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public void terminate() {
        this.terminate(0);
    }

    public void terminate(int code) {
        LOGGER.info("Infinite Machine is shutting down...");
        System.exit(code);
    }

}