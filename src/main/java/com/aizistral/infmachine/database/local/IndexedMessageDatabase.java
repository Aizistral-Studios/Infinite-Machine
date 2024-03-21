package com.aizistral.infmachine.database.local;

import com.aizistral.infmachine.config.JsonHandler;
import com.aizistral.infmachine.utils.StandardLogger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class IndexedMessageDatabase extends JsonHandler<IndexedMessageDatabase.Data> {
    private static final StandardLogger LOGGER = new StandardLogger("JSONDatabase");
    public static final IndexedMessageDatabase INSTANCE = new IndexedMessageDatabase();

    private IndexedMessageDatabase(){
        super(Paths.get("./database/indexed_message_data.json"), 600_000L, Data.class, Data::new);
        try{
            this.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------- //
    // Caching of message rating //
    // ------------------------- //
    public List<Long> getCachedMessageByID(long messageID) {
        try {
            this.readLock.lock();
            return this.getData().messageRatingCacheByMessageID.getOrDefault(messageID, new ArrayList<Long>());
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
            this.getData().messageRatingCacheByMessageID.put(messageID, value);
            this.needsSaving.set(true);
            return value;
        } finally {
            this.writeLock.unlock();
        }
    }

    public void removeCachedMessageByID(long messageID) {
        try {
            this.writeLock.lock();
            this.getData().messageRatingCacheByMessageID.remove(messageID);
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    // --------------------------- //
    // Caching of channel messages //
    // --------------------------- //

    public long getCachedMessageByChannelID(long channelID) {
        try {
            this.readLock.lock();
            return this.getData().messageCacheByChannelID.getOrDefault(channelID, -1L);
        } finally {
            this.readLock.unlock();
        }
    }

    public long setCachedMessageByChannelID(long channelID, long messageID) {
        try {
            this.writeLock.lock();
            this.getData().messageCacheByChannelID.put(channelID, messageID);
            this.needsSaving.set(true);
            return messageID;
        } finally {
            this.writeLock.unlock();
        }
    }

    public void removeCachedMessageByChannelID(long messageID) {
        try {
            this.writeLock.lock();
            this.getData().messageCacheByChannelID.remove(messageID);
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    // ------------------ //
    // Database utilities //
    // ------------------ //
    public void resetIndexation() {
        try {
            this.writeLock.lock();
            this.getData().messageRatingCacheByMessageID.clear();
            this.getData().messageCacheByChannelID.clear();
            this.needsSaving.set(true);
        } finally {
            this.writeLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data
    {
        private final HashMap<Long, ArrayList<Long>> messageRatingCacheByMessageID = new HashMap<>();
        private final HashMap<Long, Long> messageCacheByChannelID = new HashMap<>();
    }
}
