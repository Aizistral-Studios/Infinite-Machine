package com.aizistral.infmachine.database;

import java.util.List;
import java.util.Set;

import com.aizistral.infmachine.data.*;
import com.aizistral.infmachine.utils.Triple;
import com.aizistral.infmachine.utils.Tuple;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public interface MachineDatabase {

    public int getVotingCount(long userID);

    public int addVotingCount(long userID, int count);

    public Set<Voting> getVotings();

    public void addVoting(Voting voting);

    public void removeVoting(Voting voting);

    public int getMessageCount(long userID);

    public int setMessageCount(long userID, int count);

    public int addMessageCount(long userID, int count);

    public int getMessageRating(long userID);

    public int setMessageRating(long userID, int points);

    public int addMessageRating(long userID, int points);

    public boolean hasIndexedMessages(ChannelType type, long channelID);

    public void addIndexedMessage(ChannelType type, long channelID, long messageID);

    public long getIndexedMessage(ChannelType type, long channelID, int indexFromEnd);

    public void resetIndexation();

    public List<LeaderboardEntry> getTopMessageSenders(JDA jda, Guild guild, LeaderboardOrder order, int start, int limit);

    public UserRating getSenderRating(JDA jda, Guild guild, long userID);

    public String getLastVersion();

    public void setLastVersion(String version);

    public void forceSave();

}
