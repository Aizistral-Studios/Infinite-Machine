package com.aizistral.infmachine.data;

import java.util.Locale;

import com.aizistral.infmachine.InfiniteMachine;

import lombok.Value;

@Value
public class UserRating {
    private long userID;
    private int messageCount;
    private int ratingPoints;
    private int positionByMessages;
    private int positionByRating;

    public int getDispayRating() {
        return InfiniteMachine.getDispayRating(this.ratingPoints);
    }

    public String getDisplayRatingFormatted() {
        return String.format(Locale.US, "%,d", this.getDispayRating());
    }

    public String getMessageCountFormatted() {
        return String.format(Locale.US, "%,d", this.messageCount);
    }

}
