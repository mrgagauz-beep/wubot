package com.wubot;

import com.wubot.action.ActionExecutor;
import com.wubot.auth.AuthManager;
import com.wubot.auth.ClientInfo;
import com.wubot.auth.HttpAuthClient;
import com.wubot.brain.BotBrain;
import com.wubot.config.BotConfig;
import com.wubot.discovery.DiscoveryCollector;
import com.wubot.network.Connection;
import com.wubot.network.PacketSender;
import com.wubot.persistence.PersistenceManager;
import com.wubot.protocol.PacketProcessor;
import com.wubot.protocol.packets.GameStateResponsePacket;
import com.wubot.protocol.packets.MapInfoPacket;
import com.wubot.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Main application entry point.
 *
 * Authentication flow (from AUTH_FULL.md):
 * 1. Load config.json (optional, fallback to CLI args)
 * 2. HTTPS: Get token via /auth-api/v4/get-token/login
 * 3. TCP: Connect to game server (162.19.232.126:43431)
 * 4. TCP: Send ApiRequestPacket with auth/token-login
 * 5. TCP: Wait for ApiResponsePacket with SUCCESSFUL
 * 6. TCP: Wait for GameStateResponsePacket
 */
public class WuBotApplication {
    private static final Logger log = LoggerFactory.getLogger(WuBotApplication.class);

    // Default connection settings (from AUTH_FULL.md)
    private static final String DEFAULT_HOST = "162.19.232.126";
    private static final int DEFAULT_PORT = 43431;
    private static final int GAME_STATE_TIMEOUT_MS = 5000;

    private final Connection connection;
    private final PacketSender sender;
    private final World world;
    private final PacketProcessor processor;
    private final BotBrain brain;
    private final ActionExecutor executor;
    private final GameLoop gameLoop;
    private final PersistenceManager persistence;
    private final DiscoveryCollector discovery;
    private final ClientInfo clientInfo;
    private final AuthManager authManager;

    private volatile boolean shuttingDown = false;
    
    // Store initial MapInfoPacket received during auth to pass to GameLoop
    private MapInfoPacket initialMapInfo = null;

    public WuBotApplication() {
        this(ClientInfo.createDefault());
    }

    public WuBotApplication(ClientInfo clientInfo) {
        // Initialize all components
        this.connection = new Connection();
        this.sender = new PacketSender(connection);
        this.world = new World();
        this.persistence = new PersistenceManager();
        this.discovery = new DiscoveryCollector(persistence);
        this.processor = new PacketProcessor(discovery);
        this.brain = new BotBrain();
        this.executor = new ActionExecutor(sender, discovery);
        this.gameLoop = new GameLoop(connection, processor, world, brain, executor, discovery);
        this.clientInfo = clientInfo;
        this.authManager = new AuthManager(connection, clientInfo);

        // Link ship manager to packet processor
        processor.setShipManager(brain.getShipManager());
    }

    /**
     * Start the bot with pre-obtained token.
     * Token format: "tokenId:UUID"
     *
     * @param host  server hostname
     * @param port  server port
     * @param token auth token (tokenId:UUID format)
     */
    public void startWithToken(String host, int port, String token) throws IOException {
        log.info("WuBot starting...");
        log.info("Connecting to {}:{}...", host, port);

        // Connect to server
        connection.connect(host, port);

        if (!connection.isConnected()) {
            throw new IOException("Failed to connect to server");
        }

        log.info("Connected! Authenticating with token...");

        // Authenticate with token
        if (!authManager.loginWithToken(token)) {
            connection.disconnect();
            throw new IOException("Authentication failed");
        }

        // Wait for GameStateResponsePacket (as per AUTH_FULL.md)
        log.info("Waiting for game state initialization...");
        if (!waitForGameState()) {
            log.warn("GameStateResponsePacket not received within timeout, continuing anyway...");
        }

        // Load discovery data
        discovery.loadAll();

        log.info("Authentication successful! Starting game loop...");

        // Setup shutdown hook
        setupShutdownHook();

        // Process initial MapInfoPacket if received during auth
        if (initialMapInfo != null) {
            gameLoop.processInitialMapInfo(initialMapInfo);
        }

        // Start game loop (blocks)
        gameLoop.start();
    }

    /**
     * Start the bot with username/password.
     * Will first get token via HTTPS, then authenticate via TCP.
     *
     * @param host     server hostname
     * @param port     server port
     * @param username login username
     * @param password login password
     */
    public void startWithCredentials(String host, int port, String username, String password) throws IOException {
        log.info("WuBot starting...");

        // Step 1: Get token via HTTPS
        log.info("Getting auth token via HTTPS...");
        HttpAuthClient httpAuth = new HttpAuthClient();
        String token = httpAuth.authenticate(username, password);

        if (token == null) {
            throw new IOException("Failed to get auth token - check credentials");
        }

        log.info("Got token, connecting to {}:{}...", host, port);

        // Connect to server
        connection.connect(host, port);

        if (!connection.isConnected()) {
            throw new IOException("Failed to connect to server");
        }

        log.info("Connected! Authenticating...");

        // Step 2: Authenticate with token via TCP
        if (!authManager.loginWithToken(token)) {
            connection.disconnect();
            throw new IOException("Authentication failed");
        }

        // Wait for GameStateResponsePacket (as per AUTH_FULL.md)
        log.info("Waiting for game state initialization...");
        if (!waitForGameState()) {
            log.warn("GameStateResponsePacket not received within timeout, continuing anyway...");
        }

        // Load discovery data
        discovery.loadAll();

        log.info("Authentication successful! Starting game loop...");

        // Setup shutdown hook
        setupShutdownHook();

        // Process initial MapInfoPacket if received during auth
        if (initialMapInfo != null) {
            gameLoop.processInitialMapInfo(initialMapInfo);
        }

        // Start game loop (blocks)
        gameLoop.start();
    }

    /**
     * Wait for GameStateResponsePacket after authentication.
     * As per AUTH_FULL.md, server sends this automatically after successful auth.
     *
     * @return true if GameStateResponsePacket received, false on timeout
     */
    private boolean waitForGameState() {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < GAME_STATE_TIMEOUT_MS) {
            List<Object> packets = connection.pollPackets();

            for (Object packet : packets) {
                if (packet instanceof GameStateResponsePacket gameState) {
                    log.info("GameStateResponsePacket received! Player initialized.");
                    // Process the game state through normal processor
                    processor.process(gameState, world);
                    return true;
                }
                // Store MapInfoPacket for later use by GameLoop
                if (packet instanceof MapInfoPacket mapInfo) {
                    log.info("MapInfoPacket received during init: {} (id={})", mapInfo.name, mapInfo.mapId);
                    initialMapInfo = mapInfo;
                }
                // Process other packets too
                processor.process(packet, world);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return false;
    }

    /**
     * Stop the bot gracefully.
     */
    public void stop() {
        if (shuttingDown) return;
        shuttingDown = true;

        log.info("Shutting down...");

        // Stop game loop
        gameLoop.stop();

        // Save discovery data
        discovery.saveAll();

        // Disconnect
        connection.disconnect();

        log.info("Shutdown complete");
    }

    /**
     * Setup JVM shutdown hook for graceful shutdown.
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered");
            stop();
        }, "shutdown-hook"));
    }

    // === Main entry point ===

    public static void main(String[] args) {
        log.info("=== WuBot ===");

        // Step 1: Try to load config.json (as per AUTH_FULL.md)
        BotConfig config = BotConfig.load();

        // Parse CLI arguments (override config)
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        String token = null;
        String user = null;
        String pass = null;
        String uid = null;

        // Apply config values first
        if (config != null) {
            BotConfig.ConnectionConfig conn = config.getConnection();
            host = conn.getServerHost();
            port = conn.getServerPort();
            if (conn.hasToken()) {
                token = conn.getToken();
            }
            if (conn.hasCredentials()) {
                user = conn.getUsername();
                pass = conn.getPassword();
            }
            if (conn.getDeviceId() != null && !conn.getDeviceId().isEmpty()) {
                uid = conn.getDeviceId();
            }
            log.info("Config loaded: host={}, port={}, hasToken={}, hasCredentials={}",
                    host, port, conn.hasToken(), conn.hasCredentials());
        }

        // CLI args override config
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host", "-h" -> {
                    if (i + 1 < args.length) host = args[++i];
                }
                case "--port", "-p" -> {
                    if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                }
                case "--token", "-t" -> {
                    if (i + 1 < args.length) token = args[++i];
                }
                case "--user", "-u" -> {
                    if (i + 1 < args.length) user = args[++i];
                }
                case "--pass" -> {
                    if (i + 1 < args.length) pass = args[++i];
                }
                case "--uid" -> {
                    if (i + 1 < args.length) uid = args[++i];
                }
                case "--help" -> {
                    printUsage();
                    return;
                }
            }
        }

        // Create ClientInfo with config values
        ClientInfo clientInfo;
        if (uid != null) {
            clientInfo = ClientInfo.createWithUid(uid);
        } else {
            clientInfo = ClientInfo.createDefault();
        }

        // Apply config locale settings if available
        if (config != null) {
            BotConfig.ConnectionConfig conn = config.getConnection();
            clientInfo.setVersion(conn.getVersionArray());
            clientInfo.setPlatform(conn.getPlatform());
            clientInfo.setSystemLocale(conn.getSystemLocale());
            clientInfo.setPreferredLocale(conn.getPreferredLocale());
        }

        log.info("Client: {}", clientInfo);

        // Start bot
        WuBotApplication app = new WuBotApplication(clientInfo);

        try {
            if (token != null && !token.isEmpty()) {
                // Token auth mode
                log.info("Using token authentication");
                app.startWithToken(host, port, token);
            } else if (user != null && !user.isEmpty()) {
                // Credential auth mode
                if (pass == null || pass.isEmpty()) {
                    pass = promptPassword();
                }
                if (pass == null || pass.isEmpty()) {
                    log.error("Password required!");
                    System.exit(1);
                }
                log.info("Using credential authentication for user: {}", user);
                app.startWithCredentials(host, port, user, pass);
            } else {
                // Interactive mode
                log.info("No credentials in config.json or CLI, entering interactive mode");
                String[] credentials = promptCredentials();
                if (credentials == null) {
                    log.error("Credentials required!");
                    printUsage();
                    System.exit(1);
                }
                app.startWithCredentials(host, port, credentials[0], credentials[1]);
            }
        } catch (IOException e) {
            log.error("Failed to start bot: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Prompt for password from console.
     */
    private static String promptPassword() {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("Password: ");
            if (passwordChars != null) {
                return new String(passwordChars);
            }
        } else {
            // Fallback for IDE
            System.out.print("Password: ");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                return reader.readLine();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Prompt for username and password from console.
     */
    private static String[] promptCredentials() {
        Console console = System.console();
        BufferedReader reader = null;

        try {
            if (console != null) {
                String username = console.readLine("Username: ");
                char[] passwordChars = console.readPassword("Password: ");
                if (username != null && passwordChars != null) {
                    return new String[]{username, new String(passwordChars)};
                }
            } else {
                // Fallback for IDE
                reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Username: ");
                String username = reader.readLine();
                System.out.print("Password: ");
                String password = reader.readLine();
                if (username != null && password != null) {
                    return new String[]{username, password};
                }
            }
        } catch (IOException e) {
            log.error("Failed to read credentials: {}", e.getMessage());
        }

        return null;
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar wubot.jar [options]");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Create config.json in current directory (see config.json.example)");
        System.out.println("  CLI arguments override config.json values");
        System.out.println();
        System.out.println("Authentication options:");
        System.out.println("  --token, -t <token>   Auth token (tokenId:UUID format)");
        System.out.println("  --user, -u <login>    Username for HTTPS auth");
        System.out.println("  --pass <password>     Password (will prompt if not provided)");
        System.out.println();
        System.out.println("Connection options:");
        System.out.println("  --host, -h <host>     Server hostname (default: " + DEFAULT_HOST + ")");
        System.out.println("  --port, -p <port>     Server port (default: " + DEFAULT_PORT + ")");
        System.out.println();
        System.out.println("Other options:");
        System.out.println("  --uid <uuid>          Device UUID (default: random)");
        System.out.println("  --help                Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar wubot.jar                    (uses config.json)");
        System.out.println("  java -jar wubot.jar --user mylogin     (prompts for password)");
        System.out.println("  java -jar wubot.jar --token 30337147:4cce0299-15af-...");
    }
}
