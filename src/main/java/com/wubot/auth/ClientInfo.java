package com.wubot.auth;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Client information sent with auth requests.
 * Contains device ID, version, platform, and client hash.
 */
public class ClientInfo {

    /** Device UUID (generated or from preferences) */
    private String uid;

    /** Build number (usually 0) */
    private int build;

    /** Client version [major, minor, patch] */
    private int[] version;

    /** Platform: "Desktop", "Android", "iOS" */
    private String platform;

    /** System locale */
    private String systemLocale;

    /** Preferred locale for game */
    private String preferredLocale;

    /** MD5 hash of game executable (integrity check) */
    private String clientHash;

    /** Current client version */
    public static final int[] VERSION_1_233_0 = {1, 233, 0};

    /** Hash for version 1.233.0 */
    public static final String HASH_1_233_0 = "269980fe6e943c59e8ff10338f719870";

    public ClientInfo() {
        // Default values
        this.uid = UUID.randomUUID().toString();
        this.build = 0;
        this.version = VERSION_1_233_0.clone();
        this.platform = "Desktop";
        this.systemLocale = Locale.getDefault().toString();
        this.preferredLocale = "en";
        this.clientHash = HASH_1_233_0;
    }

    /**
     * Create default Desktop ClientInfo with default hash.
     */
    public static ClientInfo createDefault() {
        return new ClientInfo();
    }

    /**
     * Create ClientInfo with specific UID.
     */
    public static ClientInfo createWithUid(String uid) {
        ClientInfo info = new ClientInfo();
        info.uid = uid;
        return info;
    }

    // Getters and setters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getBuild() {
        return build;
    }

    public void setBuild(int build) {
        this.build = build;
    }

    public int[] getVersion() {
        return version;
    }

    public void setVersion(int[] version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSystemLocale() {
        return systemLocale;
    }

    public void setSystemLocale(String systemLocale) {
        this.systemLocale = systemLocale;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public String getClientHash() {
        return clientHash;
    }

    public void setClientHash(String clientHash) {
        this.clientHash = clientHash;
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "uid='" + uid + '\'' +
                ", version=" + Arrays.toString(version) +
                ", platform='" + platform + '\'' +
                ", clientHash='" + clientHash + '\'' +
                '}';
    }
}
