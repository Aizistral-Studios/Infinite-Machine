package com.aizistral.infmachine.routines;

import java.util.ArrayList;
import java.util.List;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.MachineBootstrap;
import com.aizistral.infmachine.config.GuildConfig;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ActivePunishment;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleLogger;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class RoutineHandler extends Thread {
    private static final SimpleLogger LOGGER = new SimpleLogger("RoutineHandler");
    private static final RoutineHandler INSTANCE = new RoutineHandler();
    private static final List<RoutineContainer> ROUTINES = new ArrayList<>();
    private static long cycleCounter = 0;

    @Override
    public void run() {
        LOGGER.info("Starting up routine handler...");

        while (true) {
            for (RoutineContainer container : ROUTINES) {
                if (cycleCounter % container.executionFrequency() == 0) {
                    try {
                        container.routine().execute();
                    } catch (Exception ex) {
                        LOGGER.info("Error when executing a routine:", ex);
                    }
                }
            }

            cycleCounter++;

            try {
                Thread.sleep(InfiniteConfig.getInstance().getRoutineSleepMs());
            } catch (InterruptedException ex) {
                LOGGER.error("Handler interruped, exiting the loop.");
                break;
            }
        }
    }

    public static void initialize() {
        if (!ROUTINES.isEmpty())
            throw new IllegalStateException("RoutineHandler already initialized!");

        register(new UnpunishRoutine());

        INSTANCE.start();
    }

    public static void register(Routine routine) {
        register(routine, 1);
    }

    public static void register(Routine routine, int executionFrequency) {
        ROUTINES.add(new RoutineContainer(routine, executionFrequency));
    }

}
