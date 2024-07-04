package com.aizistral.infmachine.data;

import java.util.Locale;

import com.aizistral.infmachine.InfiniteMachine;

import lombok.Value;

@Value
public class LeaderboardEntry {
    private long userID;
    private String userName;
    private int messageCount;
    private int rating;

    public int getDispayRating() {
        return InfiniteMachine.getDispayRating(this.rating);
    }

    public String getDisplayRatingFormatted() {
        return String.format(Locale.US, "%,d", this.getDispayRating());
    }

    public String getMessageCountFormatted() {
        return String.format(Locale.US, "%,d", this.messageCount);
    }

    public String getRatingFormatted() {
        return String.format(Locale.US, "%,d", this.rating);
    }
}
