package com.wubot.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wubot.network.Connection;
import com.wubot.protocol.api.ApiRequestPacket;
import com.wubot.protocol.api.ApiResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages authentication with the game server.
 *
 * Supports two auth methods:
 * 1. loginWithToken() - Uses ApiRequestPacket with uri="auth/token-login"
 * 2. Full login flow - HTTPS to get token, then TCP auth
 */
public class AuthManager {
    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    private static final Gson gson = new GsonBuilder().create();

    private static final int AUTH_TIMEOUT_MS = 30000;
    private static final String AUTH_TOKEN_LOGIN_URI = "auth/token-login";

    private final Connection connection;
    private final ClientInfo clientInfo;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private String authToken;
    private boolean authenticated = false;

    public AuthManager(Connection connection, ClientInfo clientInfo) {
        this.connection = connection;
        this.clientInfo = clientInfo;
    }

    public AuthManager(Connection connection) {
        this(connection, ClientInfo.createDefault());
    }

    /**
     * Full authentication flow:
     * 1. Get token via HTTPS
     * 2. Authenticate via TCP with token
     *
     * @param username Username
     * @param password Password
     * @return true if authentication successful
     */
    public boolean login(String username, String password) {
        log.info("Starting authentication for {}...", username);

        // Step 1: Get token via HTTPS
        HttpAuthClient httpAuth = new HttpAuthClient();
        String token = httpAuth.authenticate(username, password);

        if (token == null) {
            log.error("Failed to get token via HTTPS");
            return false;
        }

        // Step 2: Authenticate via TCP
        return loginWithToken(token);
    }

    /**
     * Authenticate with a pre-obtained token.
     *
     * @param token Auth token in format "tokenId:UUID"
     * @return true if authentication successful
     */
    public boolean loginWithToken(String token) {
        log.info("Authenticating with token...");
        this.authToken = token;

        // Build request JSON
        AuthTokenLoginRequest tokenRequest = new AuthTokenLoginRequest(token, clientInfo);
        String requestJson = gson.toJson(tokenRequest);

        log.debug("Auth request: {}", requestJson);

        // Build and send ApiRequestPacket
        int requestId = requestIdCounter.getAndIncrement();
        ApiRequestPacket apiRequest = new ApiRequestPacket(requestId, AUTH_TOKEN_LOGIN_URI, requestJson);

        connection.send(apiRequest);
        log.info("Sent auth/token-login request (id={})", requestId);

        // Wait for response
        return waitForAuthResponse(requestId);
    }

    /**
     * Wait for authentication response.
     * IMPORTANT: Non-auth packets are stored and returned to the queue after auth completes.
     */
    private boolean waitForAuthResponse(int expectedRequestId) {
        long startTime = System.currentTimeMillis();
        List<Object> savedPackets = new ArrayList<>();

        while (System.currentTimeMillis() - startTime < AUTH_TIMEOUT_MS) {
            List<Object> packets = connection.pollPackets();

            for (Object packet : packets) {
                if (packet instanceof ApiResponsePacket response) {
                    if (AUTH_TOKEN_LOGIN_URI.equals(response.getUri())) {
                        // Return saved packets to queue before returning
                        connection.returnPackets(savedPackets);
                        log.debug("Returned {} saved packets to queue after auth", savedPackets.size());
                        return processAuthResponse(response);
                    }
                }
                // Save non-auth packets to return later
                savedPackets.add(packet);
            }

            // Small delay to avoid busy waiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Return saved packets even on timeout
        connection.returnPackets(savedPackets);
        log.error("Authentication timeout after {} ms", AUTH_TIMEOUT_MS);
        return false;
    }

    /**
     * Process authentication response.
     */
    private boolean processAuthResponse(ApiResponsePacket response) {
        String data = response.getResponseDataJson();
        String info = response.getResponseInfoJson();

        log.debug("Auth response: info={}, data={}", info, data);

        // Check for SUCCESSFUL in responseData (netStatus field)
        boolean success = (data != null && data.contains("SUCCESSFUL")) ||
                          (info != null && info.contains("SUCCESSFUL"));

        if (success) {
            authenticated = true;
            log.info("Authentication successful!");
        } else {
            log.error("Authentication failed: info={}, data={}", info, data);
        }

        return success;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getAuthToken() {
        return authToken;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void reset() {
        this.authToken = null;
        this.authenticated = false;
    }

    /**
     * Internal class for AuthTokenLoginRequest JSON structure.
     */
    private static class AuthTokenLoginRequest {
        private final String token;
        private final ClientInfoJson clientInfo;

        public AuthTokenLoginRequest(String token, ClientInfo info) {
            this.token = token;
            this.clientInfo = new ClientInfoJson(info);
        }
    }

    /**
     * ClientInfo JSON structure for API requests.
     */
    private static class ClientInfoJson {
        private final String uid;
        private final int build;
        private final int[] version;
        private final String platform;
        private final String systemLocale;
        private final String preferredLocale;
        private final String clientHash;

        public ClientInfoJson(ClientInfo info) {
            this.uid = info.getUid();
            this.build = info.getBuild();
            this.version = info.getVersion();
            this.platform = info.getPlatform();
            this.systemLocale = info.getSystemLocale();
            this.preferredLocale = info.getPreferredLocale();
            this.clientHash = info.getClientHash();
        }
    }
}
