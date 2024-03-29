package com.aizistral.infmachine;

import java.io.IOException;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.utils.StandardLogger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class Main {
    private static final StandardLogger LOGGER = new StandardLogger("MachineBootstrap");
    static final JDA JDA;

    static {
        LOGGER.log("Starting up the Infinite Machine...");

        try {
            InfiniteConfig.INSTANCE.init();
            InfiniteConfig.INSTANCE.forceSave();
            Localization.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        String token = InfiniteConfig.INSTANCE.getAccessToken();

        if (token.isEmpty())
            throw new RuntimeException("Access token not specified in config.json.");

        JDABuilder builder = JDABuilder.createDefault(token);

        builder.enableIntents(
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS
                );

        builder.setActivity(Activity.watching(Localization.translate("activity.watching")));

        JDA = builder.build();
    }

    public static void main(String... args) throws Exception {
        InfiniteMachine.LOGGER.log("Exiting main method...");
    }

}
