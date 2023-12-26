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
import com.aizistral.infmachine.utils.Tuple;

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
    public int getMessageRating(long userID)
    {
        try {
            this.readLock.lock();
            return this.getData().messageRating.getOrDefault(userID, 0);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public int setMessageRating(long userID, int score)
    {
        try {
            this.writeLock.lock();
            this.getData().messageRating.put(userID, score);
            this.needsSaving.set(true);
            return score;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int addMessageRating(long userID, int score)
    {
        try {
            this.writeLock.lock();
            int newScore = this.getMessageRating(userID) + score;
            this.setMessageRating(userID, newScore);
            return newScore;
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
    @SuppressWarnings("unchecked")
    public List<LeaderboardEntry> getTopMessageSenders(JDA jda, Guild guild, LeaderboardType type, int start, int limit) {
        try {
            this.readLock.lock();
            long time = System.currentTimeMillis();

            List<LeaderboardEntry> topSenders = new ArrayList<>();
            List<CompletableFuture<String>> futuresStr = new ArrayList<>();
            List<CompletableFuture<Void>> futuresVoid = new ArrayList<>();

            List<Entry<Long, Integer>> allSendersMessages = new ArrayList<>(this.getData().messageCounts.entrySet());
            List<Entry<Long, Integer>> allSendersRating = new ArrayList<>(this.getData().messageRating.entrySet());
            allSendersMessages.sort(Entry.comparingByKey(Comparator.reverseOrder()));
            allSendersRating.sort(Entry.comparingByKey(Comparator.reverseOrder()));

            List<Triple<Long, Integer, Integer>> allSenders = new ArrayList<>();
            for (int i = 0; i < allSendersMessages.size(); i++) {
                Entry<Long, Integer> m = allSendersMessages.get(i);
                Entry<Long, Integer> r = allSendersRating.get(i);
                Long userIDM = m.getKey();
                Integer messageCount = m.getValue();
                Long userIDR = r.getKey();
                Integer rating = r.getValue();
                if(!userIDM.equals(userIDR)) {
                    LOGGER.error("ERROR: INVALID USER IDs in getTopMessageSenders");
                }
                allSenders.add(new Triple<>(userIDM, messageCount, rating));
            }

            start -= 1;
            int boardSize = Math.min(limit, allSenders.size() - start);

            LOGGER.debug("Board size: %s", boardSize);

            for (int i = start; i < boardSize + (start); i++) {
                Triple<Long, Integer, Integer> entry = allSenders.get(i);
                int pos = i - start;

                val futureStr = new CompletableFuture<String>();
                val futureVoid = futureStr.thenAccept(s -> topSenders.add(new LeaderboardEntry(entry.getA(), s, entry.getB(), entry.getC())));

                futuresStr.add(futureStr);
                futuresVoid.add(futureVoid);

                guild.retrieveMemberById(entry.getA()).queue(member -> {
                    futuresStr.get(pos).complete(member.getEffectiveName());
                }, ex -> {
                    jda.retrieveUserById(entry.getA()).queue(user -> {
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

            switch (type) {
                case MESSAGES:
                    topSenders.sort((a, b) -> {
                        return b.getMessageCount() - a.getMessageCount();
                    });
                    break;
                case RATING:
                    topSenders.sort((a, b) -> {
                        return b.getRating() - a.getRating();
                    });
                    break;
            }

            return topSenders;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public Triple<Integer, Integer, Integer> getSenderRating(JDA jda, Guild guild, long userID, LeaderboardType type) {
        try {
            this.readLock.lock();
            int rank = 1;
            int count = this.getMessageCount(userID);
            int rating = this.getMessageRating(userID);

            switch (type) {
                case MESSAGES:
                    for (val entry : this.getData().messageCounts.entrySet()) {
                        if (entry.getKey().longValue() != userID && entry.getValue().intValue() > count) {
                            rank++;
                        }
                    }
                    break;
                case RATING:
                    for (val entry : this.getData().messageRating.entrySet()) {
                        if (entry.getKey().longValue() != userID && entry.getValue().intValue() > rating) {
                            rank++;
                        }
                    }
                    break;
            }


            return new Triple<>(rank, count, rating);
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
        private final HashMap<Long, Integer> votingCounts = new HashMap<>();
        private final HashMap<Long, List<Long>> channelIndexTails = new HashMap<>();
        private final HashMap<Long, List<Long>> threadIndexTails = new HashMap<>();
        private final HashSet<Voting> votings = new HashSet<>();
        private String lastVersion = "";
    }

}
