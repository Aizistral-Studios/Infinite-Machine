package com.aizistral.infmachine.handlers;

import java.util.List;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.MachineBootstrap;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ActiveBan;
import com.aizistral.infmachine.utils.SimpleLogger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;

public class RoutineHandler extends Thread {
    private static final SimpleLogger LOGGER = new SimpleLogger("RoutineHandler");
    private static final RoutineHandler INSTANCE = new RoutineHandler();

    @Override
    public void run() {
        LOGGER.info("Starting up routine handler...");

        while (true) {
            long time = System.currentTimeMillis();
            List<ActiveBan> bans = InfiniteDatabase.getActiveBans();

            for (ActiveBan ban : bans) {
                if (time > ban.getTimestamp() + ban.getDuration()) {
                    JDA jda = MachineBootstrap.getJDA();
                    Guild guild = jda.getGuildById(ban.getGuildId());
                    long subjectId = ban.getSubjectId();
                    long guildId = ban.getGuildId();

                    if (guild != null) {
                        guild.unban(UserSnowflake.fromId(subjectId)).reason(Localization.get("msg.banExpired"))
                        .queue(success -> {
                            LOGGER.info("Subject {} unbanned in guild {} as their ban expired.", subjectId, guildId);
                            InfiniteDatabase.clearBan(subjectId, guildId);
                        }, error -> {
                            LOGGER.error("Failed to unban subject {} in guild {} after expired ban:", error);
                        });
                    }
                }
            }

            try {
                Thread.sleep(InfiniteConfig.getInstance().getRoutineSleepMs());
            } catch (InterruptedException ex) {
                LOGGER.error("Handler interruped, exiting the loop.");
                break;
            }
        }
    }

    public static void initialize() {
        INSTANCE.start();
    }

}
