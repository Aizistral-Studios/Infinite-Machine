package com.aizistral.infmachine.data;

import lombok.Value;

@Value
public class LeaderboardEntry {
    private long userID;
    private String userName;
    private int messageCount;
    private int rating;
}
