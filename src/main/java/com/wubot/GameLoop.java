package com.wubot;

import com.wubot.action.Action;
import com.wubot.action.ActionExecutor;
import com.wubot.brain.BotBrain;
import com.wubot.brain.NavigationBrain;
import com.wubot.discovery.DiscoveryCollector;
import com.wubot.network.Connection;
import com.wubot.protocol.PacketProcessor;
import com.wubot.protocol.api.ApiNotification;
import com.wubot.protocol.packets.MapInfoPacket;
import com.wubot.world.World;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Main game loop - single threaded.
 * Polls packets, processes them, makes decisions, executes actions.
 */
public class GameLoop {
    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);

    /** Tick interval in milliseconds (10 Hz) */
    public static final int TICK_INTERVAL_MS = 100;

    /** Auto-save interval in ticks (every 5 minutes at 10 Hz) */
    private static final int AUTO_SAVE_INTERVAL = 3000;

    private final Connection connection;
    private final PacketProcessor processor;
    private final World world;
    private final BotBrain brain;
    private final ActionExecutor executor;
    private final DiscoveryCollector discovery;

    private volatile boolean running = false;
    private long tickCount = 0;
    private boolean explorationStarted = false;
    
    /** Enable exploration mode on startup (for testing portal teleportation) */
    private boolean explorationModeEnabled = true;  // Enabled for testing portal teleportation
    
    /** Ship destroyed detection */
    private boolean hadValidPlayerState = false;
    private boolean shipDestroyed = false;
    private long lastRepairRequestTime = 0;
    private static final long REPAIR_REQUEST_COOLDOWN_MS = 3000;  // Don't spam repair requests

    public GameLoop(Connection connection, PacketProcessor processor, World world,
                    BotBrain brain, ActionExecutor executor, DiscoveryCollector discovery) {
        this.connection = connection;
        this.processor = processor;
        this.world = world;
        this.brain = brain;
        this.executor = executor;
        this.discovery = discovery;
    }

    /**
     * Start the game loop (blocks current thread).
     */
    public void start() {
        log.info("Starting game loop...");
        running = true;
        run();
    }

    /**
     * Stop the game loop.
     */
    public void stop() {
        log.info("Stopping game loop...");
        running = false;
        
        // Save discovery data on shutdown
        discovery.forceSave();
        log.info("Discovery data saved on shutdown: {}", discovery.getSummary());
    }

    /**
     * Main loop.
     */
    private void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            try {
                tick();
            } catch (Exception e) {
                log.error("Error in tick {}: {}", tickCount, e.getMessage(), e);
            }

            // Maintain tick rate
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < TICK_INTERVAL_MS) {
                try {
                    Thread.sleep(TICK_INTERVAL_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Game loop interrupted");
                    break;
                }
            } else if (elapsed > TICK_INTERVAL_MS * 2) {
                log.warn("Tick {} took too long: {}ms", tickCount, elapsed);
            }

            tickCount++;
        }

        log.info("Game loop stopped after {} ticks", tickCount);
    }

    /**
     * Single tick of the game loop.
     */
    private void tick() {
        // 1. Poll packets from connection
        List<Object> packets = connection.pollPackets();

        // 2. Process packets and update world
        for (Object packet : packets) {
            processor.process(packet, world);

            // Handle map changes for navigation
            if (packet instanceof MapInfoPacket mapInfo) {
                handleMapChange(mapInfo);
            }
            
            // Handle ApiNotification for map info (sent after teleportation)
            // Per Wireshark capture: Server sends map info via ApiNotification JSON after teleport
            if (packet instanceof ApiNotification notification) {
                handleApiNotification(notification);
            }
        }

        // 3. Create snapshot of world state
        WorldSnapshot snapshot = world.snapshot();
        
        // 3.5. Check for ship destroyed state and auto-repair
        if (checkAndRepairIfDestroyed(snapshot)) {
            // Ship is destroyed, skip brain decisions until repaired
            return;
        }

        // 4. Brain decides actions
        List<Action> actions = brain.decide(snapshot);

        // 5. Execute actions
        for (Action action : actions) {
            executor.execute(action);
        }

        // Periodic diagnostic logging (every 5 seconds for better visibility)
        if (tickCount % 50 == 0) {
            Integer targetId = brain.getCombatBrain().getCurrentTargetId();
            String targetInfo = "none";
            if (targetId != null) {
                var target = snapshot.findNpc(targetId);
                if (target != null) {
                    targetInfo = String.format("id=%d hp=%d/%d", targetId, target.getHp(), target.getMaxHp());
                } else {
                    targetInfo = String.format("id=%d (not found)", targetId);
                }
            }
            log.info("[DIAG] tick={} state={} pos=({},{}) hp={}/{} shield={}/{} cargo={}/{} npcs={} boxes={} target={} actions={}",
                    tickCount, brain.getState(),
                    (int) snapshot.getPlayerX(), (int) snapshot.getPlayerY(),
                    snapshot.getPlayerHp(), snapshot.getPlayerMaxHp(),
                    snapshot.getPlayerShield(), snapshot.getPlayerMaxShield(),
                    snapshot.getCargoUsed(), snapshot.getCargoMax(),
                    snapshot.getNpcs().size(), snapshot.getBoxes().size(),
                    targetInfo, actions.size());
        }

        // Auto-save discovery data periodically
        if (tickCount > 0 && tickCount % AUTO_SAVE_INTERVAL == 0) {
            discovery.saveAll();
        }
        
        // Start exploration mode if enabled (after initial ticks)
        maybeStartExploration();
    }

    /**
     * Handle map change event.
     */
    private void handleMapChange(MapInfoPacket mapInfo) {
        log.info("Map changed to: {} (id={})", mapInfo.name, mapInfo.mapId);

        // Update discovery system with map info
        WorldSnapshot snapshot = world.snapshot();
        discovery.onMapInfo(mapInfo, snapshot.getPlayerX(), snapshot.getPlayerY());

        // Force save discovery data on map change (don't wait for 5-minute autosave)
        // Use forceSave() instead of saveAll() to ensure data is saved even if dirty=false
        discovery.forceSave();
        log.info("Discovery data saved on map change: {}", discovery.getSummary());

        // Update navigation brain with portal data
        // TPort has id, type, subtype, x, y - use id for TeleportRequestPacket
        if (mapInfo.teleports != null) {
            NavigationBrain.PortalInfo[] portals = new NavigationBrain.PortalInfo[mapInfo.teleports.length];
            for (int i = 0; i < mapInfo.teleports.length; i++) {
                MapInfoPacket.TPort tp = mapInfo.teleports[i];
                // Pass tp.id for TeleportRequestPacket.portalId
                portals[i] = new NavigationBrain.PortalInfo(i, tp.id, tp.type, tp.subtype, tp.x, tp.y);
                log.debug("[NAV] Portal {}: id={}, type={}, subtype={}, pos=({},{})", 
                         i, tp.id, tp.type, tp.subtype, tp.x, tp.y);
            }
            brain.setCurrentMapPortals(mapInfo.mapId, portals);
        }

        // Let brain handle map change (for evacuation logic)
        List<Action> mapChangeActions = brain.onMapChanged(mapInfo.mapId, snapshot);
        for (Action action : mapChangeActions) {
            executor.execute(action);
        }
        
        // Notify brain of map change for exploration mode
        brain.onExplorationMapChanged(mapInfo.mapId);
    }
    
    /**
     * Handle ApiNotification packets.
     * Per Wireshark capture: Server sends map info via ApiNotification JSON after teleportation.
     * JSON format: {"mapId":5,"name":"E-2","width":16000,"height":10000,"mapObjects":[...]}
     */
    private void handleApiNotification(ApiNotification notification) {
        String key = notification.getKey();
        String json = notification.getNotificationJsonString();
        
        log.info("[API_NOTIFICATION] key='{}', json length={}", key, json != null ? json.length() : 0);
        
        // Handle map-info notification (sent after teleportation)
        if ("map-info".equals(key) && json != null && !json.isEmpty()) {
            try {
                Gson gson = new Gson();
                JsonObject mapData = gson.fromJson(json, JsonObject.class);
                
                int mapId = mapData.has("mapId") ? mapData.get("mapId").getAsInt() : -1;
                String mapName = mapData.has("name") ? mapData.get("name").getAsString() : "Unknown";
                int width = mapData.has("width") ? mapData.get("width").getAsInt() : 0;
                int height = mapData.has("height") ? mapData.get("height").getAsInt() : 0;
                
                log.info("[API_NOTIFICATION] Map info received: {} (id={}, {}x{})", mapName, mapId, width, height);
                
                // Create a MapInfoPacket from the JSON data to reuse existing handling
                MapInfoPacket mapInfo = new MapInfoPacket();
                mapInfo.mapId = mapId;
                mapInfo.name = mapName;
                mapInfo.width = width;
                mapInfo.height = height;
                
                // Parse teleports/portals from mapObjects if present
                if (mapData.has("mapObjects")) {
                    JsonArray mapObjects = mapData.getAsJsonArray("mapObjects");
                    // Count portals first
                    int portalCount = 0;
                    for (int i = 0; i < mapObjects.size(); i++) {
                        JsonObject obj = mapObjects.get(i).getAsJsonObject();
                        if (obj.has("type") && "portal".equals(obj.get("type").getAsString())) {
                            portalCount++;
                        }
                    }
                    
                    if (portalCount > 0) {
                        mapInfo.teleports = new MapInfoPacket.TPort[portalCount];
                        int portalIndex = 0;
                        for (int i = 0; i < mapObjects.size(); i++) {
                            JsonObject obj = mapObjects.get(i).getAsJsonObject();
                            if (obj.has("type") && "portal".equals(obj.get("type").getAsString())) {
                                MapInfoPacket.TPort portal = new MapInfoPacket.TPort();
                                portal.id = obj.has("id") ? obj.get("id").getAsInt() : 0;
                                portal.type = obj.has("portalType") ? obj.get("portalType").getAsInt() : 0;
                                portal.subtype = obj.has("subtype") ? obj.get("subtype").getAsInt() : 0;
                                portal.x = obj.has("x") ? obj.get("x").getAsInt() : 0;
                                portal.y = obj.has("y") ? obj.get("y").getAsInt() : 0;
                                mapInfo.teleports[portalIndex++] = portal;
                                log.debug("[API_NOTIFICATION] Portal: id={}, type={}, pos=({},{})", 
                                         portal.id, portal.type, portal.x, portal.y);
                            }
                        }
                    }
                }
                
                // Process the map info using existing handler
                handleMapChange(mapInfo);
                
            } catch (Exception e) {
                log.error("[API_NOTIFICATION] Failed to parse map-info JSON: {}", e.getMessage(), e);
            }
        } else {
            // Log other notifications for debugging
            log.debug("[API_NOTIFICATION] Unhandled notification: key='{}', json='{}'", key, 
                     json != null && json.length() > 100 ? json.substring(0, 100) + "..." : json);
        }
    }
    
    /**
     * Start exploration mode if enabled.
     * Called after world state is valid (player has position and HP).
     * IMPORTANT: Don't start exploration while world is still initializing (pos=0,0, hp=0)!
     */
    private void maybeStartExploration() {
        if (explorationModeEnabled && !explorationStarted && tickCount >= 10) {
            // Check if world state is valid before starting exploration
            WorldSnapshot snapshot = world.snapshot();
            float playerX = snapshot.getPlayerX();
            float playerY = snapshot.getPlayerY();
            int maxHp = snapshot.getPlayerMaxHp();
            
            // Wait for valid player state (non-zero position and HP)
            if (playerX == 0 && playerY == 0) {
                log.debug("[EXPLORE] Waiting for valid player position (currently at 0,0)");
                return;
            }
            if (maxHp <= 0) {
                log.debug("[EXPLORE] Waiting for valid player HP (currently maxHp={})", maxHp);
                return;
            }
            
            explorationStarted = true;
            log.info("[EXPLORE] Starting exploration mode (tick {}, pos=({},{}), maxHp={})", 
                     tickCount, (int)playerX, (int)playerY, maxHp);
            brain.startExploration();
        }
    }

    // === Getters ===

    public boolean isRunning() {
        return running;
    }

    public long getTickCount() {
        return tickCount;
    }
    
    /**
     * Process initial MapInfoPacket received during authentication.
     * Call this before start() if MapInfoPacket was received during auth.
     */
    public void processInitialMapInfo(MapInfoPacket mapInfo) {
        if (mapInfo != null) {
            log.info("Processing initial MapInfoPacket: {} (id={})", mapInfo.name, mapInfo.mapId);
            handleMapChange(mapInfo);
        }
    }
    
    /**
     * Enable or disable exploration mode.
     * When enabled, bot will teleport to another map and back on startup.
     */
    public void setExplorationModeEnabled(boolean enabled) {
        this.explorationModeEnabled = enabled;
        log.info("Exploration mode: {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Check if exploration mode is enabled.
     */
    public boolean isExplorationModeEnabled() {
        return explorationModeEnabled;
    }
    
    /**
     * Check if ship is destroyed and send repair request if needed.
     * Ship is considered destroyed when:
     * - We previously had valid player state (hadValidPlayerState = true)
     * - Current state shows hp=0, maxHp=0, position=(0,0)
     * 
     * @return true if ship is destroyed and we should skip brain decisions
     */
    private boolean checkAndRepairIfDestroyed(WorldSnapshot snapshot) {
        float playerX = snapshot.getPlayerX();
        float playerY = snapshot.getPlayerY();
        int hp = snapshot.getPlayerHp();
        int maxHp = snapshot.getPlayerMaxHp();
        
        // Track if we ever had valid player state
        if (!hadValidPlayerState && maxHp > 0 && (playerX != 0 || playerY != 0)) {
            hadValidPlayerState = true;
            shipDestroyed = false;
            log.info("[REPAIR] Valid player state detected: pos=({},{}), hp={}/{}", 
                     (int)playerX, (int)playerY, hp, maxHp);
        }
        
        // Check for destroyed state: had valid state before, now hp=0, maxHp=0, pos=(0,0)
        if (hadValidPlayerState && maxHp == 0 && hp == 0 && playerX == 0 && playerY == 0) {
            if (!shipDestroyed) {
                shipDestroyed = true;
                log.warn("[REPAIR] Ship destroyed! Sending repair request...");
            }
            
            // Send repair request with cooldown to avoid spamming
            long now = System.currentTimeMillis();
            if (now - lastRepairRequestTime >= REPAIR_REQUEST_COOLDOWN_MS) {
                lastRepairRequestTime = now;
                executor.sendRepair();
                log.info("[REPAIR] Repair request sent (cooldown={}ms)", REPAIR_REQUEST_COOLDOWN_MS);
            }
            
            return true;  // Skip brain decisions while destroyed
        }
        
        // If we were destroyed but now have valid state again, we're repaired
        if (shipDestroyed && maxHp > 0) {
            shipDestroyed = false;
            log.info("[REPAIR] Ship repaired! Resuming normal operation. pos=({},{}), hp={}/{}", 
                     (int)playerX, (int)playerY, hp, maxHp);
        }
        
        return false;
    }
}
