package com.aizistral.infmachine.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.aizistral.infmachine.utils.SimpleLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;

public class InfiniteConfig {
    private static final SimpleLogger LOGGER = new SimpleLogger("InfiniteConfig");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Path FILE = Paths.get("./config/config.json");

    @Getter
    private static InfiniteConfig instance = null;

    @Getter
    private String accessToken = "";

    public static void load() throws IOException {
        LOGGER.info("Retreiving the config file...");

        Files.createDirectories(FILE.getParent());

        if (!Files.exists(FILE)) {
            LOGGER.info("No config file found, creating a new one...", FILE);
            instance = new InfiniteConfig();
        } else {
            if (!Files.isRegularFile(FILE))
                throw new IOException("Path " + FILE + " exists, but is not a file!");

            try (BufferedReader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                instance = GSON.fromJson(reader, InfiniteConfig.class);
            } catch (Exception ex) {
                LOGGER.error("Could not read config file: {}", FILE);
                LOGGER.error("This likely indicates the file is corrupted.");
                throw ex;
            }

            LOGGER.info("Config file contents loaded.");
        }

        save();
    }

    public static void save() throws IOException {
        if (instance == null)
            throw new IOException("Cannot save the config file when instance is null!");

        try (BufferedWriter writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(instance, writer);
        }

        LOGGER.info("Config file saved to: {}", FILE);
    }

}
