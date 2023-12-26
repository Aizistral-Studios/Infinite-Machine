package com.aizistral.infmachine.data;

public class LeaderboardEntry
{
    private Long userID;
    private String userName;
    int messageCount;
    int rating;

    public LeaderboardEntry(Long userID, String userName, int messageCount, int rating)
    {
        setUserID(userID);
        setUserName(userName);
        setMessageCount(messageCount);
        setRating(rating);
    }

    public Long getUserID()
    {
        return userID;
    }

    public void setUserID(Long userID)
    {
        this.userID = userID;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public int getMessageCount()
    {
        return messageCount;
    }

    public void setMessageCount(int messageCount)
    {
        this.messageCount = messageCount;
    }

    public int getRating()
    {
        return rating;
    }

    public void setRating(int rating)
    {
        this.rating = rating;
    }
}
