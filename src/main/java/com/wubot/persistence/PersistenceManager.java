package com.wubot.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages JSON persistence for discovery data.
 */
public class PersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);

    private static final String DATA_DIR = "data";

    private final Gson gson;
    private final Path dataPath;

    public PersistenceManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.dataPath = Paths.get(DATA_DIR);

        // Ensure data directory exists
        ensureDataDirectory();
    }

    /**
     * Ensure data directory exists.
     */
    private void ensureDataDirectory() {
        try {
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.info("Created data directory: {}", dataPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create data directory: {}", e.getMessage(), e);
        }
    }

    /**
     * Save object to JSON file.
     *
     * @param filename file name (without path)
     * @param object object to serialize
     */
    public void saveJson(String filename, Object object) {
        Path filePath = dataPath.resolve(filename);
        try {
            String json = gson.toJson(object);
            Files.writeString(filePath, json);
            log.debug("Saved {} ({} bytes)", filename, json.length());
        } catch (IOException e) {
            log.error("Failed to save {}: {}", filename, e.getMessage(), e);
        }
    }

    /**
     * Load object from JSON file.
     *
     * @param filename file name (without path)
     * @param clazz class to deserialize to
     * @return deserialized object or null if failed
     */
    public <T> T loadJson(String filename, Class<T> clazz) {
        Path filePath = dataPath.resolve(filename);
        try {
            if (!Files.exists(filePath)) {
                log.debug("File {} does not exist, returning null", filename);
                return null;
            }

            String json = Files.readString(filePath);
            T result = gson.fromJson(json, clazz);
            log.debug("Loaded {} ({} bytes)", filename, json.length());
            return result;
        } catch (IOException e) {
            log.error("Failed to load {}: {}", filename, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Failed to parse {}: {}", filename, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if file exists.
     */
    public boolean exists(String filename) {
        return Files.exists(dataPath.resolve(filename));
    }

    /**
     * Get Gson instance for custom serialization.
     */
    public Gson getGson() {
        return gson;
    }
}
