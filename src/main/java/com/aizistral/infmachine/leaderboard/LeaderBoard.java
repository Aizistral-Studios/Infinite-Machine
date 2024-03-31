package com.aizistral.infmachine.leaderboard;

import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.utils.StandardLogger;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class LeaderBoard {
    private static final StandardLogger LOGGER = new StandardLogger("Leaderboard");
    public static final LeaderBoard INSTANCE = new LeaderBoard();

    private LeaderBoard() {

    }

    public WebhookMessageCreateAction getLeaderboardString(SlashCommandInteractionEvent event)
    {
        return event.getHook().sendMessage("Here will be a leaderboard").setAllowedMentions(Collections.EMPTY_LIST);
    }

    @NotNull
    public WebhookMessageCreateAction getRatingString(SlashCommandInteractionEvent event) {
        /*OptionMapping mapping = event.getOption("user");
        User user = null;
        String message = null;

        if (mapping != null && mapping.getAsUser() != event.getUser()) {
            user = mapping.getAsUser();
            val rating = this.database.getSenderRating(this.jda, this.domain, user.getIdLong());
            message = Localization.translate("msg.rating", user.getIdLong(), rating.getPositionByRating(),
                    rating.getDisplayRatingFormatted(), rating.getPositionByMessages(),
                    rating.getMessageCountFormatted());
        } else {
            user = event.getUser();
            val rating = this.database.getSenderRating(this.jda, this.domain, user.getIdLong());
            message = Localization.translate("msg.ratingOwn", rating.getPositionByRating(),
                    rating.getDisplayRatingFormatted(), rating.getPositionByMessages(),
                    rating.getMessageCountFormatted());
        }*/

        return event.getHook().sendMessage("Here you can see rating soon").setAllowedMentions(Collections.EMPTY_LIST);
    }

    /*

                if (order == LeaderboardOrder.MESSAGES) {
                    val option = event.getOption("start");
                    int start = option != null ? Math.max(option.getAsInt(), 1) : 1;

                    val senders = this.getDatabase().getTopMessageSenders(this.jda, this.domain, order, start, 10);
                    String reply = "";

                    if (start == 1) {
                        reply += Localization.translate("msg.leaderboardHeader") + "\n";
                    } else {
                        reply += Localization.translate("msg.leaderboardHeaderAlt", start, start + 9) + "\n";
                    }

                    for (int i = 0; i < senders.size(); i++) {
                        val leaderboardEntry = senders.get(i);
                        reply += "\n" + Localization.translate("msg.leaderboardEntryMessages", i + start,
                                leaderboardEntry.getUserName(), leaderboardEntry.getUserID(),
                                leaderboardEntry.getMessageCountFormatted(),
                                leaderboardEntry.getDisplayRatingFormatted());
                    }

                    return event.getHook().sendMessage(reply).setAllowedMentions(Collections.EMPTY_LIST);
                } else if (order == LeaderboardOrder.RATING) {
                    val option = event.getOption("start");
                    int start = option != null ? Math.max(option.getAsInt(), 1) : 1;

                    val senders = this.getDatabase().getTopMessageSenders(this.jda, this.domain, order, start, 10);
                    String reply = "";

                    if (start == 1) {
                        reply += Localization.translate("msg.leaderboardHeader") + "\n";
                    } else {
                        reply += Localization.translate("msg.leaderboardHeaderAlt", start, start + 9) + "\n";
                    }

                    for (int i = 0; i < senders.size(); i++) {
                        val leaderboardEntry = senders.get(i);
                        reply += "\n" + Localization.translate("msg.leaderboardEntryRating", i + start,
                                leaderboardEntry.getUserName(), leaderboardEntry.getUserID(),
                                leaderboardEntry.getDisplayRatingFormatted(),
                                leaderboardEntry.getMessageCountFormatted());
                    }

     */

    /*
    public static int getDispayRating(int points) {
        int segmentLength = 50;
        return points / (segmentLength * segmentLength);
    }
     */
}
