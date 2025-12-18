package com.wubot.protocol.api;

/**
 * API request packet sent over TCP.
 * Used for auth/token-login and other API endpoints.
 */
public class ApiRequestPacket {

    private int requestId;
    private String uri;
    private String requestDataJson;

    public ApiRequestPacket() {
    }

    public ApiRequestPacket(int requestId, String uri, String requestDataJson) {
        this.requestId = requestId;
        this.uri = uri;
        this.requestDataJson = requestDataJson;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRequestDataJson() {
        return requestDataJson;
    }

    public void setRequestDataJson(String requestDataJson) {
        this.requestDataJson = requestDataJson;
    }

    @Override
    public String toString() {
        return "ApiRequestPacket{" +
                "requestId=" + requestId +
                ", uri='" + uri + '\'' +
                ", requestDataJson='" + requestDataJson + '\'' +
                '}';
    }
}
