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
import java.util.concurrent.CompletionException;

import org.jetbrains.annotations.Nullable;

import com.aizistral.infmachine.config.AsyncJSONConfig;
import com.aizistral.infmachine.data.ChannelType;
import com.aizistral.infmachine.data.IndexationMode;
import com.aizistral.infmachine.data.Voting;
import com.aizistral.infmachine.database.MachineDatabase;
import com.aizistral.infmachine.utils.Triple;
import com.aizistral.infmachine.utils.Tuple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class JSONDatabase extends AsyncJSONConfig<JSONDatabase.Data> implements MachineDatabase {
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
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Nullable
    private Member getGuildMember(Guild guild, long userID) {
        Member member = null;

        try {
            member = guild.retrieveMemberById(userID).useCache(true).submit().join();
        } catch (CompletionException ex) {
            // NO-OP
        }

        return member;
    }

    private boolean isGuildMember(Guild guild, long userID) {
        return this.getGuildMember(guild, userID) != null;
    }

    @Override
    public List<Triple<Long, String, Integer>> getTopMessageSenders(Guild guild, int limit) {
        try {
            this.readLock.lock();
            List<Triple<Long, String, Integer>> topSenders = new ArrayList<>();
            List<Entry<Long, Integer>> allSenders = new ArrayList<>(this.getData().messageCounts.entrySet());
            allSenders.sort(Entry.comparingByValue(Comparator.reverseOrder()));

            for (int i = 0; i < limit && i < allSenders.size(); i++) {
                Entry<Long, Integer> entry = allSenders.get(i);
                Member member = this.getGuildMember(guild, entry.getKey());

                if (member == null) {
                    limit++;
                    continue;
                }

                topSenders.add(new Triple<>(entry.getKey(), member.getEffectiveName(), entry.getValue()));
            }

            return topSenders;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public Tuple<Integer, Integer> getSenderRating(Guild guild, long userID) {
        try {
            this.readLock.lock();
            int rating = 1;
            int count = this.getMessageCount(userID);

            for (val entry : this.getData().messageCounts.entrySet()) {
                if (entry.getKey().longValue() != userID && entry.getValue().intValue() > count) {
                    if (this.isGuildMember(guild, entry.getKey().longValue())) {
                        rating++;
                    }
                }
            }

            return new Tuple<>(rating, count);
        } finally {
            this.readLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data {
        private final HashMap<Long, Integer> messageCounts = new HashMap<>();
        private final HashMap<Long, Integer> votingCounts = new HashMap<>();
        private final HashMap<Long, List<Long>> channelIndexTails = new HashMap<>();
        private final HashMap<Long, List<Long>> threadIndexTails = new HashMap<>();
        private final HashSet<Voting> votings = new HashSet<>();
    }

}
