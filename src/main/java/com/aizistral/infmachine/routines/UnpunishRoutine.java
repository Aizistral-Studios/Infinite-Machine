package com.aizistral.infmachine.routines;

import java.util.List;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.config.GuildConfig;
import com.aizistral.infmachine.config.Lang;
import com.aizistral.infmachine.database.InfiniteDatabase;
import com.aizistral.infmachine.database.model.ActivePunishment;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleLogger;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class UnpunishRoutine implements Routine {
    private static final SimpleLogger LOGGER = new SimpleLogger("UnpunishRoutine");

    @Override
    public void execute() {
        long time = System.currentTimeMillis();
        List<ActivePunishment> punishments = InfiniteDatabase.getActivePunishments();

        for (ActivePunishment punishment : punishments) {
            if (punishment.getDuration() <= 0) {
                continue;
            }

            if (time > punishment.getTimestamp() + punishment.getDuration()) {
                InfiniteMachine.getInstanceFor(punishment.getGuildId()).ifPresent(machine -> {
                    long subjectId = punishment.getSubjectId();
                    long guildId = punishment.getGuildId();
                    int caseId = punishment.getCaseId();

                    if (punishment.getType() == Type.BAN) {
                        machine.getGuild().unban(UserSnowflake.fromId(subjectId)).reason(Lang.get("audit.banExpired"))
                        .queue(success -> {
                            machine.getModerationLogChannel().ifPresent(channel -> {
                                channel.sendMessageEmbeds(this.getUnbanEmbed(machine.getConfig(), punishment)).queue();
                            });

                            LOGGER.info("Subject {} unbanned in guild {} as their ban expired.", subjectId, guildId);
                            InfiniteDatabase.clearBan(subjectId, guildId);
                        }, error -> {
                            if (error instanceof ErrorResponseException ex) {
                                if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                                    LOGGER.error("Failed to unban subject {} in guild {}: subject not banned.", subjectId,
                                            guildId);
                                    InfiniteDatabase.clearBan(subjectId, guildId);
                                    return;
                                }
                            }

                            LOGGER.error("Failed to unban subject {} in guild {} after expired ban:", error, subjectId,
                                    guildId);
                        });
                    } else if (punishment.getType() == Type.WARNING) {
                        LOGGER.info("Warning #{} removed from subject {} in guild {} since it expired.", caseId, subjectId,
                                guildId);
                        InfiniteDatabase.clearWarning(caseId, guildId);
                    }
                });
            }
        }

    }

    private MessageEmbed getUnbanEmbed(GuildConfig config, ActivePunishment punishment) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(config.getEmbedNormalColor());
        builder.setTitle(config.getUnpunishedEmoji() + " " + Lang.get("msg.unbanTitle"));
        builder.setDescription(Lang.get("msg.unbanDesc", "<@" + punishment.getSubjectId() + ">"));

        return builder.build();
    }

}
