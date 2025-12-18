package com.wubot.protocol.api;

/**
 * API notification packet from server.
 */
public class ApiNotification {

    private String key;
    private String notificationJsonString;

    public ApiNotification() {
    }

    public ApiNotification(String key, String notificationJsonString) {
        this.key = key;
        this.notificationJsonString = notificationJsonString;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNotificationJsonString() {
        return notificationJsonString;
    }

    public void setNotificationJsonString(String notificationJsonString) {
        this.notificationJsonString = notificationJsonString;
    }

    @Override
    public String toString() {
        return "ApiNotification{" +
                "key='" + key + '\'' +
                ", notificationJsonString='" + notificationJsonString + '\'' +
                '}';
    }
}
