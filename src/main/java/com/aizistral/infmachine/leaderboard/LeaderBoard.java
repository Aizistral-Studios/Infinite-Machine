package com.aizistral.infmachine.leaderboard;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.indexation.CoreMessageIndexer;
import com.aizistral.infmachine.utils.StandardLogger;
import lombok.val;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LeaderBoard {
    private static final StandardLogger LOGGER = new StandardLogger("Leaderboard");
    public static final LeaderBoard INSTANCE = new LeaderBoard();

    private LeaderBoard() {

    }

    public String getLeaderboardString(SlashCommandInteractionEvent event, boolean isAdmin)
    {
        val option = event.getOption("start");
        int start = option != null ? Math.max(option.getAsInt(), 1) : 1;
        StringBuilder stringBuilder = new StringBuilder();
        String headerString = String.format("## %s", start == 1 ? "Top 10 Most Active Human-Like Entities:": String.format("Most Active Human-Like Entities (Positions %d - %d):", start, start + 9));
        stringBuilder.append(headerString + "\n");
        List<Map<String, Object>> leaderBoard = getLeaderboard(start, isAdmin);
        int i = start;
        for (Map<String, Object> entry : leaderBoard) {
            long authorID = Long.parseLong(entry.get("authorID").toString());
            User user = InfiniteMachine.INSTANCE.getJDA().retrieveUserById(authorID).complete();
            String name = user.getEffectiveName();
            stringBuilder.append(String.format("%d. **%s** (<@%d>): ", i++, name, user.getIdLong()));
            Long messages = Long.parseLong(entry.get("totalMessages").toString());
            Long rating = getDispayRating(Long.parseLong(entry.get("totalRating").toString()));
            if(isAdmin) {
                stringBuilder.append(String.format("%d messages", messages));
                stringBuilder.append(" | ");
                stringBuilder.append(String.format("%d rating ", rating));
            } else {
                stringBuilder.append(String.format("%d rating ", rating));
                stringBuilder.append(" | ");
                stringBuilder.append(String.format("%d messages", messages));
            }
            stringBuilder.append("\n");

        }
        return stringBuilder.toString();
    }

    public String getRatingString(SlashCommandInteractionEvent event) {
        val option = event.getOption("user");
        long userID = option != null ? option.getAsUser().getIdLong() : event.getUser().getIdLong();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(userID == event.getUser().getIdLong() ? "## Your Rating Stats:" : String.format("## <@%d>'s Rating Stats:", userID));
        stringBuilder.append("\n");
        Map<String, Object> entry = getRating(userID).get(0);
        stringBuilder.append(String.format("Leaderboard position: **#%s**\n", entry.get("position").toString()));
        long rating = getDispayRating(Long.parseLong(entry.get("totalRating").toString()));
        stringBuilder.append(String.format("Total rating acquired: **%d points**\n", rating));
        long messageCount = Long.parseLong(entry.get("totalMessages").toString());
        stringBuilder.append(String.format("Total messages sent: **%d messages**\n",messageCount));
        stringBuilder.append(String.format("Rating to messages ratio: **%.2f**\n", 1.0 * rating / messageCount));
        return stringBuilder.toString();
    }

    private List<Map<String,Object>> getLeaderboard(int start, boolean isAdmin) {
        String sql = String.format("SELECT authorID, totalRating, totalMessages\n" +
                        "FROM (\n" +
                        "    SELECT authorID, SUM(messageRating) AS totalRating, COUNT(*) AS totalMessages\n" +
                        "    FROM %s\n" +
                        "    GROUP BY authorID\n" +
                        "    HAVING totalRating > 0 AND authorID > 0\n" +
                        ") AS scores\n" +
                        "ORDER BY %s DESC\n" +
                        "LIMIT 10 OFFSET %d;",CoreMessageIndexer.INSTANCE.getIndexTableName(), isAdmin ? "totalMessages" : "totalRating", start - 1);
         return DataBaseHandler.INSTANCE.executeQuerySQL(sql);
    }

    private List<Map<String,Object>> getRating(long authorID) {
        String sql = String.format("SELECT authorID, totalRating, totalMessages, position\n" +
                "FROM (\n" +
                "   SELECT authorID, totalRating, totalMessages, RANK() OVER (ORDER BY totalRating DESC) AS position\n" +
                "   FROM (\n" +
                "   SELECT authorID, SUM(messageRating) AS totalRating, COUNT(*) AS totalMessages\n" +
                "       FROM %s\n" +
                "       GROUP BY authorID\n" +
                "       HAVING totalRating > 0 AND authorID > 0\n" +
                "   ) AS scores\n" +
                ") AS leaderboard\n" +
                "WHERE authorID = %d",CoreMessageIndexer.INSTANCE.getIndexTableName(), authorID);
        return DataBaseHandler.INSTANCE.executeQuerySQL(sql);
    }


    public static long getDispayRating(long points) {
        int factor = 50;
        return points / (factor * factor);
    }
}
