package com.aizistral.infmachine;

import java.io.IOException;
import java.util.List;

import com.aizistral.infmachine.commands.CommandRegistry;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.utils.SimpleLogger;

import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class MachineBootstrap {
    public static final long STARTUP_TIME = System.currentTimeMillis();
    private static final SimpleLogger LOGGER = new SimpleLogger("MachineBootstrap");
    private static JDA jda = null;

    public static void main(String... args) throws Exception {
        LOGGER.info("Starting up the Infinite Machine...");

        try {
            InfiniteConfig.load();
            Localization.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        String token = InfiniteConfig.getInstance().getAccessToken();

        if (token.isEmpty())
            throw new RuntimeException("Access token not specified in config.json.");

        JDABuilder builder = JDABuilder.createDefault(token);

        builder.enableIntents(
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS
                );

        builder.setActivity(Activity.watching(Localization.get("activity.watching")));

        jda = builder.build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // remove old global commands first
        jda.updateCommands().queue(list -> {
            jda.getGuilds().forEach(InfiniteMachine::bootInstance);
        });

        LOGGER.info("Exiting main method...");
    }

    public static String getVersion() {
        String version = MachineBootstrap.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public static void terminate() {
        terminate(0);
    }

    public static void terminate(int code) {
        LOGGER.info("Infinite Machine is shutting down...");
        System.exit(code);
    }

}
