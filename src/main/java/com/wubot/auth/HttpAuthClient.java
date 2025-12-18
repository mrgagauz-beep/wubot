package com.wubot.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTPS client for WarUniverse API authentication.
 * Gets auth token via HTTPS before TCP connection.
 */
public class HttpAuthClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAuthClient.class);

    private static final String API_HOST = "eu.api.waruniverse.space";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;

    public HttpAuthClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Authenticate with login/password via HTTPS.
     * Returns token in format "tokenId:token" if successful, null otherwise.
     *
     * @param login    Username
     * @param password Password
     * @return Combined token "tokenId:token" or null on failure
     */
    public String authenticate(String login, String password) {
        log.info("Authenticating via HTTPS as {}...", login);

        try {
            // URL encode parameters
            String encodedLogin = URLEncoder.encode(login, StandardCharsets.UTF_8);
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);

            String url = "https://" + API_HOST + "/auth-api/v4/get-token/login?login="
                    + encodedLogin + "&password=" + encodedPassword;

            log.debug("Auth URL: {}", url.replace(encodedPassword, "***"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "WarUniverse/1.233.0")
                    .GET()
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Auth response: status={}", response.statusCode());
            log.debug("Response body: {}", response.body());

            if (response.statusCode() == 200) {
                return parseTokenResponse(response.body());
            } else {
                log.error("Auth failed with status {}: {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Auth request failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Parse token response and return in format "tokenId:token".
     * Input: {"token":"4cce0299-15af-4c6e-b6df-7239643396d4","tokenId":30337147,"registered":false}
     * Output: "30337147:4cce0299-15af-4c6e-b6df-7239643396d4"
     */
    private String parseTokenResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("token") && json.has("tokenId")) {
                String token = json.get("token").getAsString();
                long tokenId = json.get("tokenId").getAsLong();

                String fullToken = tokenId + ":" + token;
                log.info("Got token: {}:{}...", tokenId, token.substring(0, Math.min(8, token.length())));
                return fullToken;
            }

            // Check for error
            if (json.has("error")) {
                log.error("API error: {}", json.get("error").getAsString());
            }

        } catch (Exception e) {
            log.error("Failed to parse token response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Test connection to API server.
     */
    public boolean testConnection() {
        try {
            String url = "https://" + API_HOST + "/";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("API server test: status={}", response.statusCode());
            return response.statusCode() > 0;

        } catch (Exception e) {
            log.error("API server not reachable: {}", e.getMessage());
            return false;
        }
    }
}
