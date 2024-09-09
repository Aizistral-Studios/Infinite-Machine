package com.aizistral.infmachine.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.database.model.ActiveBan;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleDuration;
import com.aizistral.infmachine.utils.SimpleLogger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import static com.mongodb.client.model.Filters.*;

public class InfiniteDatabase {
    private static final SimpleLogger LOGGER = new SimpleLogger("InfiniteDatabase");
    private static String mongoURI;
    private static MongoClient client;
    private static MongoDatabase machineDB;
    private static MongoCollection<ModerationAction> modActions;
    private static MongoCollection<ActiveBan> activeBans;

    public static void initialize() {
        CodecProvider provider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry registry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(provider));

        mongoURI = InfiniteConfig.getInstance().getMongoURI();
        client = MongoClients.create(mongoURI);
        machineDB = client.getDatabase("infinite-machine").withCodecRegistry(registry);
        modActions = machineDB.getCollection("moderation-actions", ModerationAction.class);
        activeBans = machineDB.getCollection("active-bans", ActiveBan.class);
    }

    private static void handleIOException(Exception ex) {
        LOGGER.error("Failed database operation:", ex);
    }

    public static boolean logAction(ModerationAction action) {
        try {
            boolean result = modActions.insertOne(action).wasAcknowledged();
            LOGGER.info("Logged moderation action: {}, acknowledged: {}", action, result);
            return result;
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static boolean registerBan(ModerationAction action, long subjectId, long guildId) {
        if (!action.isSuccess()) {
            LOGGER.error("Attempted to register failed ban, action: {}", action);
            return false;
        }

        try {
            ActiveBan ban = new ActiveBan(action.getId(), guildId, subjectId, action.getTimestamp(),
                    action.getDuration());
            boolean result = activeBans.insertOne(ban).wasAcknowledged();

            LOGGER.info("Registered active ban: {}, acknowledged: {}", ban, result);
            return result;
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static boolean clearBan(long subjectId, long guildId) {
        try {
            var result = activeBans.deleteOne(and(eq("subjectId", subjectId), eq("guildId", guildId)));
            LOGGER.info("Cleared active ban for subject {} in guild {}, deletion count: {}", subjectId, guildId,
                    result.getDeletedCount());

            return result.wasAcknowledged();
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static List<ActiveBan> getActiveBans() {
        List<ActiveBan> bans = new ArrayList<>();
        activeBans.find().into(bans);
        return ImmutableList.copyOf(bans);
    }

}
