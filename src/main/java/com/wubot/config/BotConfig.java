package com.wubot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Bot configuration loaded from config.json.
 * Format matches AUTH_FULL.md specification.
 */
public class BotConfig {
    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Ship configuration IDs
    public static final int FARM_CONFIG = 1;    // Config 1: damage (farming)
    public static final int ESCAPE_CONFIG = 2;  // Config 2: speed + defense (escape)

    private ConnectionConfig connection;

    // Default values from AUTH_FULL.md
    public static class ConnectionConfig {
        private String serverHost = "162.19.232.126";
        private int serverPort = 43431;
        private String username;
        private String password;

        private String apiHost = "eu.api.waruniverse.space";
        private int apiPort = 443;

        private String token;
        private String deviceId;

        private List<Integer> clientVersion = List.of(1, 233, 0);
        private String platform = "Desktop";
        private String systemLocale = "ru_MD";
        private String preferredLocale = "ru";

        public String getServerHost() { return serverHost; }
        public int getServerPort() { return serverPort; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getApiHost() { return apiHost; }
        public int getApiPort() { return apiPort; }
        public String getToken() { return token; }
        public String getDeviceId() { return deviceId; }
        public List<Integer> getClientVersion() { return clientVersion; }
        public String getPlatform() { return platform; }
        public String getSystemLocale() { return systemLocale; }
        public String getPreferredLocale() { return preferredLocale; }

        public int[] getVersionArray() {
            if (clientVersion == null || clientVersion.size() < 3) {
                return new int[]{1, 233, 0};
            }
            return new int[]{clientVersion.get(0), clientVersion.get(1), clientVersion.get(2)};
        }

        public boolean hasToken() {
            return token != null && !token.isEmpty();
        }

        public boolean hasCredentials() {
            return username != null && !username.isEmpty() &&
                   password != null && !password.isEmpty();
        }
    }

    public ConnectionConfig getConnection() {
        if (connection == null) {
            connection = new ConnectionConfig();
        }
        return connection;
    }

    /**
     * Load configuration from config.json file.
     *
     * @return loaded config or null if file not found
     */
    public static BotConfig load() {
        return load(Path.of("config.json"));
    }

    /**
     * Load configuration from specified path.
     *
     * @param configPath path to config file
     * @return loaded config or null if file not found
     */
    public static BotConfig load(Path configPath) {
        if (!Files.exists(configPath)) {
            log.info("Config file not found: {}", configPath);
            return null;
        }

        try (FileReader reader = new FileReader(configPath.toFile())) {
            BotConfig config = gson.fromJson(reader, BotConfig.class);
            log.info("Config loaded from {}", configPath);
            return config;
        } catch (IOException e) {
            log.error("Failed to load config from {}: {}", configPath, e.getMessage());
            return null;
        }
    }

    /**
     * Create default configuration.
     */
    public static BotConfig createDefault() {
        BotConfig config = new BotConfig();
        config.connection = new ConnectionConfig();
        return config;
    }
}
