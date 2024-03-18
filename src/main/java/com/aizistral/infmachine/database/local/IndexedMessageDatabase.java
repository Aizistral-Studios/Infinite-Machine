package com.aizistral.infmachine.database.local;

import com.aizistral.infmachine.config.AsyncJSONConfig;
import com.aizistral.infmachine.data.*;
import com.aizistral.infmachine.utils.StandardLogger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Paths;
import java.util.*;

public class IndexedMessageDatabase extends AsyncJSONConfig<IndexedMessageDatabase.Data>{
    private static final StandardLogger LOGGER = new StandardLogger("JSONDatabase");
    public static final IndexedMessageDatabase INSTANCE = new IndexedMessageDatabase();

    private IndexedMessageDatabase() {
        super(Paths.get("./database/indexed_message_data.json"), 600_000L, Data.class, Data::new);
    }

    public List<Long> getCachedMessageByID(long messageID) {
        try {
            this.readLock.lock();
            return this.getData().messageRatingCacheByID.getOrDefault(messageID, new ArrayList<Long>());
        } finally {
            this.readLock.unlock();
        }
    }

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

    public boolean hasIndexedMessages(ChannelType type, long channelID) {
        return this.getIndexedMessage(type, channelID, 0) >= 0;
    }

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

    public void resetIndexation() {
        try {
            this.writeLock.lock();
            this.getData().channelIndexTails.clear();
            this.getData().threadIndexTails.clear();
            this.getData().messageRatingCacheByID.clear();
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data
    {
        private final HashMap<Long, Integer> messageCounts = new HashMap<>();
        private final HashMap<Long, Integer> messageRating = new HashMap<>();
        private final HashMap<Long, ArrayList<Long>> messageRatingCacheByID = new HashMap<>();
        private final HashMap<Long, List<Long>> channelIndexTails = new HashMap<>();
        private final HashMap<Long, List<Long>> threadIndexTails = new HashMap<>();
    }

}
