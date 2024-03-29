package com.aizistral.infmachine.database.local;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.aizistral.infmachine.data.*;

import com.aizistral.infmachine.config.AsyncJSONConfig;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class JSONDatabase extends AsyncJSONConfig<JSONDatabase.Data> implements MachineDatabase {
    private static final StandardLogger LOGGER = new StandardLogger("JSONDatabase");
    public static final JSONDatabase INSTANCE = new JSONDatabase();

    private JSONDatabase() {
        super(Paths.get("./database/data.json"), 600_000L, Data.class, Data::new);
    }

    @Override
    public int getVotingCount(long userID) {
        try {
            this.readLock.lock();
            return this.getData().votingCounts.getOrDefault(userID, 0);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public int addVotingCount(long userID, int count) {
        try {
            this.writeLock.lock();
            int oldCount = this.getVotingCount(userID);
            int newCount = oldCount + count;
            this.getData().votingCounts.put(userID, newCount);
            return newCount;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public Set<Voting> getVotings() {
        try {
            this.readLock.lock();
            return Collections.unmodifiableSet(new HashSet<>(this.getData().votings));
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void addVoting(Voting voting) {
        try {
            this.writeLock.lock();
            this.getData().votings.add(voting);
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void removeVoting(Voting voting) {
        try {
            this.writeLock.lock();
            this.getData().votings.remove(voting);
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getMessageCount(long userID) {
        try {
            this.readLock.lock();
            return this.getData().messageCounts.getOrDefault(userID, 0);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public int setMessageCount(long userID, int count) {
        try {
            this.writeLock.lock();
            this.getData().messageCounts.put(userID, count);
            this.needsSaving.set(true);
            return count;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int addMessageCount(long userID, int count) {
        try {
            this.writeLock.lock();
            int newCount = this.getMessageCount(userID) + count;
            this.setMessageCount(userID, newCount);
            return newCount;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getMessageRating(long userID) {
        try {
            this.readLock.lock();
            return this.getData().messageRating.getOrDefault(userID, 0);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public int setMessageRating(long userID, int points) {
        try {
            this.writeLock.lock();
            this.getData().messageRating.put(userID, points);
            this.needsSaving.set(true);
            return points;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int addMessageRating(long userID, int points) {
        try {
            this.writeLock.lock();
            int newScore = this.getMessageRating(userID) + points;
            this.setMessageRating(userID, newScore);
            return newScore;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public List<Long> getCachedMessageByID(long messageID) {
        try {
            this.readLock.lock();
            return this.getData().messageRatingCacheByID.getOrDefault(messageID, new ArrayList<Long>());
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public List<Long> setCachedMessageByID(long messageID, long userID, long points) {
        try {
            this.writeLock.lock();
            ArrayList<Long> value = new ArrayList<>();
            value.add(userID);
            value.add(points);
            this.getData().messageRatingCacheByID.put(messageID, value);
            this.needsSaving.set(true);
            return value;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean hasIndexedMessages(ChannelType type, long channelID) {
        return this.getIndexedMessage(type, channelID, 0) >= 0;
    }

    @Override
    public long getIndexedMessage(ChannelType type, long channelID, int indexFromEnd) {
        try {
            this.readLock.lock();
            Map<Long, List<Long>> map = type == ChannelType.THREAD ? this.getData().threadIndexTails
                    : this.getData().channelIndexTails;
            List<Long> channelList = map.get(channelID);

            if (channelList != null && channelList.size() > 0)
                if (channelList.size() <= indexFromEnd)
                    return -1L;
                else
                    return channelList.get(channelList.size() - 1 - indexFromEnd);
            else
                return -1L;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void addIndexedMessage(ChannelType type, long channelID, long messageID) {
        try {
            this.writeLock.lock();
            Map<Long, List<Long>> map = type == ChannelType.THREAD ? this.getData().threadIndexTails
                    : this.getData().channelIndexTails;
            List<Long> channelList = map.get(channelID);

            if (channelList == null) {
                channelList = new ArrayList<>();
                map.put(channelID, channelList);
            }

            channelList.add(messageID);
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void resetIndexation() {
        try {
            this.writeLock.lock();
            this.getData().channelIndexTails.clear();
            this.getData().threadIndexTails.clear();
            this.getData().messageCounts.clear();
            this.getData().messageRating.clear();
            this.getData().messageRatingCacheByID.clear();
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    //    @Nullable
    //    private Member getGuildMember(Guild guild, long userID) {
    //        Member member = null;
    //
    //        try {
    //            member = guild.retrieveMemberById(userID).useCache(true).submit().join();
    //        } catch (CompletionException ex) {
    //            // NO-OP
    //        }
    //
    //        return member;
    //    }
    //
    //    private boolean isGuildMember(Guild guild, long userID) {
    //        return this.getGuildMember(guild, userID) != null;
    //    }

    @Override
    public List<LeaderboardEntry> getTopMessageSenders(JDA jda, Guild guild, LeaderboardOrder order, int start, int limit) {
        try {
            this.readLock.lock();
            List<LeaderboardEntry> leaderboard = new ArrayList<>();

            switch (order) {
            case MESSAGES:
                List<Triple<Long, String, Integer>> messageList = this.getTopUsers(jda, guild, start, limit,
                        this.getData().messageCounts);

                for (val entry : messageList) {
                    leaderboard.add(new LeaderboardEntry(entry.getA(), entry.getB(), entry.getC(),
                            this.getData().messageRating.getOrDefault(entry.getA(), 0)));
                }

                return leaderboard;
            case RATING:
                List<Triple<Long, String, Integer>> ratingList = this.getTopUsers(jda, guild, start, limit,
                        this.getData().messageRating);

                for (val entry : ratingList) {
                    leaderboard.add(new LeaderboardEntry(entry.getA(), entry.getB(),
                            this.getData().messageCounts.getOrDefault(entry.getA(), 0), entry.getC()));
                }

                return leaderboard;

            default:
                throw new IllegalArgumentException("Unhandled leaderboard order: " + order);
            }
        } finally {
            this.readLock.unlock();
        }
    }

    public List<Triple<Long, String, Integer>> getTopUsers(JDA jda, Guild guild, int start, int limit, Map<Long, Integer> userMap) {
        long time = System.currentTimeMillis();
        List<Entry<Long, Integer>> allSenders = new ArrayList<>(userMap.entrySet());
        List<Triple<Long, String, Integer>> topSenders = new ArrayList<>();
        allSenders.sort(Entry.comparingByValue(Comparator.reverseOrder()));

        List<CompletableFuture<String>> futuresStr = new ArrayList<>();
        List<CompletableFuture<Void>> futuresVoid = new ArrayList<>();

        start -= 1;
        int boardSize = Math.min(limit, allSenders.size() - start);

        LOGGER.debug("Board size: %s", boardSize);

        for (int i = start; i < boardSize + (start); i++) {
            Entry<Long, Integer> entry = allSenders.get(i);
            int pos = i - start;

            val futureStr = new CompletableFuture<String>();
            val futureVoid = futureStr
                    .thenAccept(s -> topSenders.add(new Triple<>(entry.getKey(), s, entry.getValue())));

            futuresStr.add(futureStr);
            futuresVoid.add(futureVoid);

            guild.retrieveMemberById(entry.getKey()).queue(member -> {
                futuresStr.get(pos).complete(member.getEffectiveName());
            }, ex -> {
                jda.retrieveUserById(entry.getKey()).queue(user -> {
                    futuresStr.get(pos).complete(user.getEffectiveName());
                }, ex2 -> {
                    futuresStr.get(pos).complete("Unknown");
                });
            });
        }

        LOGGER.debug("Time to set up leaderboard requests: %s millis", System.currentTimeMillis() - time);

        time = System.currentTimeMillis();
        futuresVoid.forEach(CompletableFuture::join);
        LOGGER.debug("Time to process leaderboard requests: %s millis", System.currentTimeMillis() - time);

        topSenders.sort((a, b) -> {
            return b.getC() - a.getC();
        });

        return topSenders;
    }

    @Override
    public UserRating getSenderRating(JDA jda, Guild guild, long userID) {
        try {
            this.readLock.lock();
            int msgsPosition = 1;
            int ratingPosition = 1;

            int msgs = this.getMessageCount(userID);
            int rating = this.getMessageRating(userID);

            for (val entry : this.getData().messageCounts.entrySet()) {
                if (entry.getKey().longValue() != userID && entry.getValue().intValue() > msgs) {
                    msgsPosition++;
                }
            }

            for (val entry : this.getData().messageRating.entrySet()) {
                if (entry.getKey().longValue() != userID && entry.getValue().intValue() > rating) {
                    ratingPosition++;
                }
            }

            return new UserRating(userID, msgs, rating, msgsPosition, ratingPosition);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public String getLastVersion() {
        try {
            this.readLock.lock();
            return this.getData().lastVersion;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void setLastVersion(String version) {
        try {
            this.writeLock.lock();
            this.getData().lastVersion = version;
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data {
        private final HashMap<Long, Integer> messageCounts = new HashMap<>();
        private final HashMap<Long, Integer> messageRating = new HashMap<>();
        private final HashMap<Long, ArrayList<Long>> messageRatingCacheByID = new HashMap<>();
        private final HashMap<Long, Integer> votingCounts = new HashMap<>();
        private final HashMap<Long, List<Long>> channelIndexTails = new HashMap<>();
        private final HashMap<Long, List<Long>> threadIndexTails = new HashMap<>();
        private final HashSet<Voting> votings = new HashSet<>();
        private String lastVersion = "";
    }

}
