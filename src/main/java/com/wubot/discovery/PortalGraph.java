package com.wubot.discovery;

import com.google.gson.reflect.TypeToken;
import com.wubot.persistence.PersistenceManager;
import com.wubot.protocol.packets.MapInfoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graph of portals and connections between maps.
 * Used for pathfinding between maps.
 */
public class PortalGraph {
    private static final Logger log = LoggerFactory.getLogger(PortalGraph.class);

    private static final String FILENAME = "portals.json";

    private final PersistenceManager persistence;

    /** Portals indexed by map ID */
    private final Map<Integer, List<PortalData>> portalsByMap;

    /** Portal connections: key = "mapId-portalIndex" */
    private final Map<String, PortalConnection> connections;

    /**
     * Portal data holder.
     */
    public static class PortalData {
        public int portalIndex;
        public int mapId;
        public float x;
        public float y;
        public int type;
        public int subtype;
        public Integer targetMapId;     // null if unknown
        public Integer targetPortalIndex;
        public int useCount;

        public PortalData() {}

        public PortalData(int portalIndex, int mapId, float x, float y, int type, int subtype) {
            this.portalIndex = portalIndex;
            this.mapId = mapId;
            this.x = x;
            this.y = y;
            this.type = type;
            this.subtype = subtype;
            this.useCount = 0;
        }
    }

    /**
     * Portal connection data.
     */
    public static class PortalConnection {
        public int fromMapId;
        public int fromPortalIndex;
        public int toMapId;
        public int toPortalIndex;

        public PortalConnection() {}

        public PortalConnection(int fromMapId, int fromPortalIndex, int toMapId, int toPortalIndex) {
            this.fromMapId = fromMapId;
            this.fromPortalIndex = fromPortalIndex;
            this.toMapId = toMapId;
            this.toPortalIndex = toPortalIndex;
        }
    }

    /**
     * Serialization wrapper.
     */
    public static class PortalGraphData {
        public Map<Integer, List<PortalData>> portalsByMap;
        public Map<String, PortalConnection> connections;
    }

    public PortalGraph(PersistenceManager persistence) {
        this.persistence = persistence;
        this.portalsByMap = new ConcurrentHashMap<>();
        this.connections = new ConcurrentHashMap<>();
    }

    /**
     * Discover portals on a map from MapInfoPacket.
     *
     * @return true if any NEW portals were discovered
     */
    public boolean discoverPortals(int mapId, MapInfoPacket.TPort[] teleports) {
        if (teleports == null || teleports.length == 0) {
            return false;
        }

        List<PortalData> existingPortals = portalsByMap.get(mapId);
        if (existingPortals == null) {
            existingPortals = new ArrayList<>();
            portalsByMap.put(mapId, existingPortals);
        }

        boolean newPortalsFound = false;

        // Add or update portals
        for (int i = 0; i < teleports.length; i++) {
            MapInfoPacket.TPort tp = teleports[i];

            // Check if portal already exists at this position
            boolean found = false;
            for (PortalData existing : existingPortals) {
                if (existing.portalIndex == i) {
                    // Update existing
                    existing.x = tp.x;
                    existing.y = tp.y;
                    existing.type = tp.type;
                    existing.subtype = tp.subtype;
                    found = true;
                    break;
                }
            }

            if (!found) {
                // New portal
                PortalData newPortal = new PortalData(i, mapId, tp.x, tp.y, tp.type, tp.subtype);
                existingPortals.add(newPortal);
                log.info("🚪 NEW PORTAL on map {} at ({}, {})", mapId, tp.x, tp.y);
                newPortalsFound = true;
            }
        }

        return newPortalsFound;
    }

    /**
     * Discover a portal connection after teleportation.
     * Records ONLY one direction (from → to).
     * Reverse connection will be recorded when player jumps back.
     *
     * @param fromMapId source map
     * @param fromPortalIndex source portal index
     * @param toMapId destination map
     * @param toPortalIndex destination portal index (portal we're standing on after teleport)
     * @return true if this was a NEW connection
     */
    public boolean discoverConnection(int fromMapId, int fromPortalIndex, int toMapId, int toPortalIndex) {
        String key = fromMapId + "-" + fromPortalIndex;

        PortalConnection existing = connections.get(key);
        if (existing == null) {
            // New connection!
            PortalConnection conn = new PortalConnection(fromMapId, fromPortalIndex, toMapId, toPortalIndex);
            connections.put(key, conn);

            log.info("🔗 CONNECTION: Map {} Portal {} → Map {} Portal {}",
                    fromMapId, fromPortalIndex, toMapId, toPortalIndex);

            // Update portal data with target
            List<PortalData> portals = portalsByMap.get(fromMapId);
            if (portals != null) {
                for (PortalData portal : portals) {
                    if (portal.portalIndex == fromPortalIndex) {
                        portal.targetMapId = toMapId;
                        portal.targetPortalIndex = toPortalIndex;
                        portal.useCount++;
                        break;
                    }
                }
            }
            return true;
        } else {
            // Connection already known, just increment use count
            List<PortalData> portals = portalsByMap.get(fromMapId);
            if (portals != null) {
                for (PortalData portal : portals) {
                    if (portal.portalIndex == fromPortalIndex) {
                        portal.useCount++;
                        break;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Find path from one map to another using BFS.
     *
     * @param fromMapId starting map
     * @param toMapId destination map
     * @return list of portals to use, or null if no path found
     */
    public List<PortalData> findPath(int fromMapId, int toMapId) {
        if (fromMapId == toMapId) {
            return Collections.emptyList();
        }

        // BFS
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, PortalData> parent = new HashMap<>();

        queue.add(fromMapId);
        parent.put(fromMapId, null);

        while (!queue.isEmpty()) {
            int current = queue.poll();

            if (current == toMapId) {
                // Found! Reconstruct path
                return reconstructPath(parent, fromMapId, toMapId);
            }

            // Get portals on current map
            List<PortalData> portals = portalsByMap.get(current);
            if (portals == null) continue;

            for (PortalData portal : portals) {
                if (portal.targetMapId != null && !parent.containsKey(portal.targetMapId)) {
                    parent.put(portal.targetMapId, portal);
                    queue.add(portal.targetMapId);
                }
            }
        }

        // No path found
        return null;
    }

    /**
     * Reconstruct path from BFS parent map.
     */
    private List<PortalData> reconstructPath(Map<Integer, PortalData> parent, int fromMapId, int toMapId) {
        List<PortalData> path = new ArrayList<>();

        Integer current = toMapId;
        while (current != null && current != fromMapId) {
            PortalData portal = parent.get(current);
            if (portal == null) break;
            path.add(0, portal);
            current = portal.mapId;
        }

        return path;
    }

    /**
     * Get portals on a map.
     */
    public List<PortalData> getPortals(int mapId) {
        return portalsByMap.getOrDefault(mapId, Collections.emptyList());
    }

    /**
     * Get connection for a portal.
     */
    public PortalConnection getConnection(int mapId, int portalIndex) {
        return connections.get(mapId + "-" + portalIndex);
    }

    /**
     * Get total portal count.
     */
    public int getPortalCount() {
        return portalsByMap.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Get connection count.
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Save to JSON.
     */
    public void save() {
        PortalGraphData data = new PortalGraphData();
        data.portalsByMap = new HashMap<>(portalsByMap);
        data.connections = new HashMap<>(connections);
        persistence.saveJson(FILENAME, data);
        log.debug("Saved {} portals, {} connections", getPortalCount(), connections.size());
    }

    /**
     * Load from JSON.
     */
    public void load() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("data", FILENAME);
            if (java.nio.file.Files.exists(path)) {
                String json = java.nio.file.Files.readString(path);
                PortalGraphData data = persistence.getGson().fromJson(json, PortalGraphData.class);
                if (data != null) {
                    portalsByMap.clear();
                    connections.clear();
                    if (data.portalsByMap != null) {
                        portalsByMap.putAll(data.portalsByMap);
                    }
                    if (data.connections != null) {
                        connections.putAll(data.connections);
                    }
                    log.info("Loaded {} portals, {} connections from {}",
                            getPortalCount(), connections.size(), FILENAME);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load portals: {}", e.getMessage(), e);
        }
    }
}
