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

@Getter
public class InfiniteConfig {
    private static final Path FILE = Paths.get("./config/config.json");
    private static final JSONLoader<InfiniteConfig> LOADER = new JSONLoader<>(FILE, InfiniteConfig.class, InfiniteConfig::new);

    @Getter
    private static InfiniteConfig instance = null;

    private String accessToken = "";
    private String mongoURI = "";
    private int embedColorDefault = 0x14D294;
    private int embedColorError = 0xD21452;

    public static void load() throws IOException {
        instance = LOADER.load();
    }

    public static void save() throws IOException {
        LOADER.save();
    }

}
