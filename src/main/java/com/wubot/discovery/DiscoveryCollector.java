package com.wubot.discovery;

import com.wubot.persistence.PersistenceManager;
import com.wubot.protocol.packets.MapInfoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Central coordinator for discovery system.
 * Collects information about maps, portals, and NPCs during gameplay.
 */
public class DiscoveryCollector {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryCollector.class);

    private final PersistenceManager persistence;
    private final MapRegistry maps;
    private final PortalGraph portals;
    private final NpcDatabase npcs;

    /** Teleportation tracking */
    private int lastMapId = -1;
    private int lastPortalIndex = -1;

    /** Dirty flag - true if there are unsaved changes */
    private volatile boolean dirty = false;

    public DiscoveryCollector(PersistenceManager persistence) {
        this.persistence = persistence;
        this.maps = new MapRegistry(persistence);
        this.portals = new PortalGraph(persistence);
        this.npcs = new NpcDatabase(persistence);
    }

    /**
     * Process MapInfoPacket - discover map and portals.
     * Call this when receiving MapInfoPacket.
     *
     * @param packet the map info packet
     * @param playerX player X position after map change
     * @param playerY player Y position after map change
     */
    public void onMapInfo(MapInfoPacket packet, float playerX, float playerY) {
        // Discover map
        if (maps.discover(packet)) {
            dirty = true;
        }

        // Discover portals on this map
        if (portals.discoverPortals(packet.mapId, packet.teleports)) {
            dirty = true;
        }

        // Check if this was a teleportation
        if (lastMapId != -1 && lastMapId != packet.mapId && lastPortalIndex != -1) {
            // Find portal we arrived at (nearest to player position)
            int arrivedPortalIndex = findNearestPortalIndex(packet.teleports, playerX, playerY);

            if (arrivedPortalIndex >= 0) {
                // Record ONE-WAY connection (from → to)
                // Reverse connection will be recorded when bot jumps back
                if (portals.discoverConnection(lastMapId, lastPortalIndex,
                        packet.mapId, arrivedPortalIndex)) {
                    dirty = true;
                }
            }
        }

        // Update tracking
        lastMapId = packet.mapId;
        lastPortalIndex = -1; // Reset after processing
    }

    /**
     * Call BEFORE teleporting through a portal.
     * This records which portal we're jumping from.
     *
     * @param portalIndex the portal index we're about to use
     */
    public void onBeforeTeleport(int portalIndex) {
        lastPortalIndex = portalIndex;
        log.debug("Preparing to teleport through portal index {}", portalIndex);
    }

    /**
     * Record NPC spawn/update.
     *
     * @param mapId map where NPC was seen
     * @param npcType NPC type ID
     * @param hp current HP
     * @param maxHp maximum HP
     * @param speed NPC speed
     */
    public void onNpcSpawn(int mapId, int npcType, int hp, int maxHp, float speed) {
        if (npcs.discover(npcType, mapId, hp, maxHp, speed)) {
            dirty = true;
        }
    }

    /**
     * Record NPC kill.
     *
     * @param npcType NPC type that was killed
     */
    public void onNpcKilled(int npcType) {
        npcs.recordKill(npcType);
        dirty = true;  // Kill stats should be saved
    }

    /**
     * Find nearest portal to given position.
     * After teleport, player is standing directly on the arrival portal.
     */
    private int findNearestPortalIndex(MapInfoPacket.TPort[] teleports, float x, float y) {
        if (teleports == null || teleports.length == 0) {
            return -1;
        }

        int nearestIndex = -1;
        float nearestDist = Float.MAX_VALUE;

        for (int i = 0; i < teleports.length; i++) {
            MapInfoPacket.TPort tp = teleports[i];
            float dx = tp.x - x;
            float dy = tp.y - y;
            float dist = dx * dx + dy * dy; // Squared distance (faster)

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    /**
     * Get map registry.
     */
    public MapRegistry getMaps() {
        return maps;
    }

    /**
     * Get portal graph.
     */
    public PortalGraph getPortals() {
        return portals;
    }

    /**
     * Get NPC database.
     */
    public NpcDatabase getNpcs() {
        return npcs;
    }

    /**
     * Get current map ID being tracked.
     */
    public int getCurrentMapId() {
        return lastMapId;
    }

    /**
     * Save all discovery data (only if dirty).
     */
    public void saveAll() {
        if (!dirty) {
            log.debug("No changes to save");
            return;
        }

        maps.save();
        portals.save();
        npcs.save();
        dirty = false;
        log.info("Discovery data saved: {} maps, {} portals, {} connections, {} NPC types",
                maps.getMapCount(), portals.getPortalCount(),
                portals.getConnectionCount(), npcs.getNpcTypeCount());
    }

    /**
     * Force save all discovery data (ignores dirty flag).
     */
    public void forceSave() {
        maps.save();
        portals.save();
        npcs.save();
        dirty = false;
        log.info("Discovery data force-saved: {} maps, {} portals, {} connections, {} NPC types",
                maps.getMapCount(), portals.getPortalCount(),
                portals.getConnectionCount(), npcs.getNpcTypeCount());
    }

    /**
     * Check if there are unsaved changes.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Load all discovery data.
     */
    public void loadAll() {
        maps.load();
        portals.load();
        npcs.load();
        log.info("Discovery data loaded: {} maps, {} portals, {} connections, {} NPC types",
                maps.getMapCount(), portals.getPortalCount(),
                portals.getConnectionCount(), npcs.getNpcTypeCount());
    }

    /**
     * Get summary of discovered data.
     */
    public String getSummary() {
        return String.format("Discovery: %d maps, %d portals (%d connections), %d NPC types",
                maps.getMapCount(), portals.getPortalCount(),
                portals.getConnectionCount(), npcs.getNpcTypeCount());
    }
}
