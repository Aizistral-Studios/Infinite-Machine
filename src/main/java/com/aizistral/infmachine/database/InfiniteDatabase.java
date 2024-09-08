package com.aizistral.infmachine.database;

import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.database.model.ModerationAction;
import com.aizistral.infmachine.database.model.ModerationAction.Type;
import com.aizistral.infmachine.utils.SimpleLogger;
import com.google.common.collect.Lists;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class InfiniteDatabase {
    private static final SimpleLogger LOGGER = new SimpleLogger("InfiniteDatabase");
    private static String mongoURI;
    private static MongoClient client;
    private static MongoDatabase machineDB;
    private static MongoCollection<ModerationAction> modActions;

    public static void initialize() {
        CodecProvider provider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry registry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(provider));

        mongoURI = InfiniteConfig.getInstance().getMongoURI();
        client = MongoClients.create(mongoURI);
        machineDB = client.getDatabase("infinite-machine").withCodecRegistry(registry);
        modActions = machineDB.getCollection("moderation-actions", ModerationAction.class);
    }

    public static boolean logAction(ModerationAction action) {
        boolean result = modActions.insertOne(action).wasAcknowledged();
        LOGGER.info("Logged moderation action: {}, acknowledged: {}", action, result);
        return result;
    }

}
