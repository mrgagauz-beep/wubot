package com.wubot;

import com.wubot.action.Action;
import com.wubot.action.ActionExecutor;
import com.wubot.brain.BotBrain;
import com.wubot.brain.NavigationBrain;
import com.wubot.discovery.DiscoveryCollector;
import com.wubot.network.Connection;
import com.wubot.protocol.PacketProcessor;
import com.wubot.protocol.packets.MapInfoPacket;
import com.wubot.world.World;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        }

        // 3. Create snapshot of world state
        WorldSnapshot snapshot = world.snapshot();

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
        if (mapInfo.teleports != null) {
            NavigationBrain.PortalInfo[] portals = new NavigationBrain.PortalInfo[mapInfo.teleports.length];
            for (int i = 0; i < mapInfo.teleports.length; i++) {
                MapInfoPacket.TPort tp = mapInfo.teleports[i];
                portals[i] = new NavigationBrain.PortalInfo(i, tp.type, tp.subtype, tp.x, tp.y);
            }
            brain.setCurrentMapPortals(mapInfo.mapId, portals);
        }

        // Let brain handle map change (for evacuation logic)
        List<Action> mapChangeActions = brain.onMapChanged(mapInfo.mapId, snapshot);
        for (Action action : mapChangeActions) {
            executor.execute(action);
        }
    }

    // === Getters ===

    public boolean isRunning() {
        return running;
    }

    public long getTickCount() {
        return tickCount;
    }
}
