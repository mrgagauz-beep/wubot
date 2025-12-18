package com.wubot.protocol.api;

/**
 * API response packet received over TCP.
 */
public class ApiResponsePacket {

    private int requestId;
    private String uri;
    private String responseInfoJson;
    private String responseDataJson;

    public ApiResponsePacket() {
    }

    public ApiResponsePacket(int requestId, String uri, String responseInfoJson, String responseDataJson) {
        this.requestId = requestId;
        this.uri = uri;
        this.responseInfoJson = responseInfoJson;
        this.responseDataJson = responseDataJson;
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

    public String getResponseInfoJson() {
        return responseInfoJson;
    }

    public void setResponseInfoJson(String responseInfoJson) {
        this.responseInfoJson = responseInfoJson;
    }

    public String getResponseDataJson() {
        return responseDataJson;
    }

    public void setResponseDataJson(String responseDataJson) {
        this.responseDataJson = responseDataJson;
    }

    @Override
    public String toString() {
        return "ApiResponsePacket{" +
                "requestId=" + requestId +
                ", uri='" + uri + '\'' +
                ", responseInfoJson='" + responseInfoJson + '\'' +
                ", responseDataJson='" + responseDataJson + '\'' +
                '}';
    }
}
