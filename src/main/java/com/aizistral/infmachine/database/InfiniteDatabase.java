package com.aizistral.infmachine.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.database.model.ActivePunishment;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.database.model.WarningTracker;
import com.aizistral.infmachine.utils.SimpleDuration;
import com.aizistral.infmachine.utils.SimpleLogger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class InfiniteDatabase {
    private static final SimpleLogger LOGGER = new SimpleLogger("InfiniteDatabase");
    private static String mongoURI;
    private static MongoClient client;
    private static MongoDatabase machineDB;
    private static MongoCollection<ModerationAction> modActions;
    private static MongoCollection<ActivePunishment> activePunishments;
    private static MongoCollection<WarningTracker> warningData;

    public static void initialize() {
        CodecProvider provider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry registry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(provider));

        mongoURI = InfiniteConfig.getInstance().getMongoURI();
        client = MongoClients.create(mongoURI);
        machineDB = client.getDatabase("infinite-machine").withCodecRegistry(registry);
        modActions = machineDB.getCollection("moderation-actions", ModerationAction.class);
        activePunishments = machineDB.getCollection("active-punishments", ActivePunishment.class);
        warningData = machineDB.getCollection("warning-data", WarningTracker.class);
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

    public static boolean registerPunishment(ModerationAction action, long subjectId, long guildId, int caseId) {
        if (!action.isSuccess()) {
            LOGGER.error("Attempted to register failed punishment, action: {}", action);
            return false;
        }

        try {
            ActivePunishment punishment = new ActivePunishment(action.getId(), caseId, guildId, subjectId,
                    action.getTimestamp(), action.getDuration(), action.getType());
            boolean result = activePunishments.insertOne(punishment).wasAcknowledged();

            LOGGER.info("Registered active punishment: {}, acknowledged: {}", punishment, result);
            return result;
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static boolean clearBan(long subjectId, long guildId) {
        try {
            var result = activePunishments.deleteOne(and(eq("type", ModerationAction.Type.BAN), eq("subjectId", subjectId),
                    eq("guildId", guildId)));
            LOGGER.info("Cleared active ban for subject {} in guild {}, deletion count: {}", subjectId, guildId,
                    result.getDeletedCount());

            return result.wasAcknowledged();
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static boolean clearWarning(int caseId, long guildId) {
        try {
            var result = activePunishments.deleteOne(and(eq("type", ModerationAction.Type.WARNING), eq("caseId", caseId),
                    eq("guildId", guildId)));
            LOGGER.info("Cleared active warning #{} in guild {}, deletion count: {}", caseId, guildId,
                    result.getDeletedCount());

            return result.wasAcknowledged();
        } catch (Exception ex) {
            handleIOException(ex);
            return false;
        }
    }

    public static int getGlobalWarningCount(long guildId) {
        return getUserWarningCount(0, guildId);
    }

    public static int setGlobalWarningCount(long guildId, int count) {
        return setUserWarningCount(0, guildId, count);
    }

    public static int incrementGlobalWarningCount(long guildId) {
        return incrementUserWarningCount(0, guildId);
    }

    public static int getUserWarningCount(long subjectId, long guildId) {
        try {
            WarningTracker tracker = warningData.find(and(eq("subjectId", subjectId), eq("guildId", guildId))).first();
            return tracker != null ? tracker.getWarningCount() : 0;
        } catch (Exception ex) {
            handleIOException(ex);
            return 0;
        }
    }

    public static int setUserWarningCount(long subjectId, long guildId, int count) {
        return updateUserWarningCount(subjectId, guildId, set("warningCount", count));
    }

    public static int incrementUserWarningCount(long subjectId, long guildId) {
        return updateUserWarningCount(subjectId, guildId, inc("warningCount", 1));
    }

    private static int updateUserWarningCount(long subjectId, long guildId, Bson update) {
        try {
            return warningData.findOneAndUpdate(and(eq("subjectId", subjectId), eq("guildId", guildId)), update,
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)).getWarningCount();
        } catch (Exception ex) {
            handleIOException(ex);
            return 0;
        }
    }

    public static List<ActivePunishment> getActivePunishments() {
        try {
            List<ActivePunishment> punishments = new ArrayList<>();
            activePunishments.find().into(punishments);
            return ImmutableList.copyOf(punishments);
        } catch (Exception ex) {
            handleIOException(ex);
            return Collections.EMPTY_LIST;
        }
    }

}
