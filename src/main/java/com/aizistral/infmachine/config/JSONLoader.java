package com.aizistral.infmachine.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.aizistral.infmachine.utils.SimpleLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONLoader<T> {
    private static final SimpleLogger LOGGER = new SimpleLogger("JSONLoader");
    protected static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Path file;
    private final Class<T> dataClass;
    private final Supplier<T> dataFactory;
    private T data = null;

    public JSONLoader(Path file, Class<T> dataClass, Supplier<T> dataFactory) {
        this.file = file;
        this.dataClass = dataClass;
        this.dataFactory = dataFactory;
    }

    public T getData() throws IllegalStateException {
        if (this.data == null)
            throw new IllegalStateException("Data requested with no instance loaded!");

        return this.data;
    }

    public T load() throws IOException {
        LOGGER.info("Retreiving config file {}...", this.file);

        Files.createDirectories(this.file.getParent());

        if (!Files.exists(this.file)) {
            LOGGER.info("Config file {} not found, creating a new one...", this.file);
            this.data = this.dataFactory.get();
        } else {
            if (!Files.isRegularFile(this.file))
                throw new IOException("Path " + this.file + " exists, but is not a file!");

            try (BufferedReader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                this.data = GSON.fromJson(reader, this.dataClass);
            } catch (Exception ex) {
                LOGGER.error("Could not read config file: {}", this.file);
                LOGGER.error("This likely indicates the file is corrupted.");
                throw ex;
            }

            LOGGER.info("Contents of file {} loaded successfully.", this.file);
        }

        return this.save();
    }

    public T save() throws IOException {
        if (this.data == null)
            throw new IOException("Cannot save the config file when data instance is null!");

        try (BufferedWriter writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(this.data, writer);
        }

        LOGGER.info("Config file {} saved successfully.", this.file);
        return this.data;
    }

}
