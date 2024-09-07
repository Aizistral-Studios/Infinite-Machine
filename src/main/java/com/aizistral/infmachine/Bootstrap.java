package com.aizistral.infmachine;

import java.io.IOException;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.utils.SimpleLogger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class Bootstrap {
    private static final SimpleLogger LOGGER = new SimpleLogger("MachineBootstrap");
    public static final JDA JDA;

    static {
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

        JDA = builder.build();

        try {
            JDA.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        JDA.updateCommands().addCommands(
                Commands.slash("ping", Localization.get("cmd.ping.desc")).addOption(null, token, token)
                ).queue();
    }

    public static void main(String... args) throws Exception {
        InfiniteMachine.LOGGER.info("Exiting main method...");
    }

}
