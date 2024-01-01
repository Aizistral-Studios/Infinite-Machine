package com.aizistral.infmachine.data;

import lombok.Value;

@Value
public class UserRating {
    private long userID;
    private int messageCount;
    private int ratingPoints;
    private int positionByMessages;
    private int positionByRating;
}
