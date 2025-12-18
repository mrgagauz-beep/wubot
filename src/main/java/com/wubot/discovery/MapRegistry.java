package com.wubot.discovery;

import com.google.gson.reflect.TypeToken;
import com.wubot.persistence.PersistenceManager;
import com.wubot.protocol.packets.MapInfoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of discovered maps.
 */
public class MapRegistry {
    private static final Logger log = LoggerFactory.getLogger(MapRegistry.class);

    private static final String FILENAME = "maps.json";

    private final PersistenceManager persistence;
    private final Map<Integer, MapData> maps;

    /**
     * Map data holder.
     */
    public static class MapData {
        public int id;
        public String name;
        public int width;
        public int height;
        public boolean hasSpaceStation;
        public float stationX;
        public float stationY;
        public long firstSeen;
        public long lastVisit;
        public int visitCount;

        public MapData() {}

        public MapData(int id, String name) {
            this.id = id;
            this.name = name;
            this.firstSeen = System.currentTimeMillis();
            this.lastVisit = this.firstSeen;
            this.visitCount = 1;
        }
    }

    public MapRegistry(PersistenceManager persistence) {
        this.persistence = persistence;
        this.maps = new ConcurrentHashMap<>();
    }

    /**
     * Discover/update map from MapInfoPacket.
     *
     * @return true if this was a NEW map (data changed), false if just revisit
     */
    public boolean discover(MapInfoPacket packet) {
        MapData existing = maps.get(packet.mapId);

        if (existing == null) {
            // New map discovered!
            MapData newMap = new MapData(packet.mapId, packet.name);
            newMap.width = packet.width;
            newMap.height = packet.height;
            newMap.hasSpaceStation = packet.spaceStation;
            newMap.stationX = packet.ssx;
            newMap.stationY = packet.ssy;

            maps.put(packet.mapId, newMap);
            log.info("🗺️ NEW MAP: {} (id={})", packet.name, packet.mapId);
            return true;
        } else {
            // Update existing map
            existing.lastVisit = System.currentTimeMillis();
            existing.visitCount++;

            // Update info in case it changed
            existing.name = packet.name;
            existing.width = packet.width;
            existing.height = packet.height;
            existing.hasSpaceStation = packet.spaceStation;
            existing.stationX = packet.ssx;
            existing.stationY = packet.ssy;

            log.debug("Map revisited: {} (visit #{})", packet.name, existing.visitCount);
            return false;
        }
    }

    /**
     * Get map data by ID.
     */
    public MapData getMap(int mapId) {
        return maps.get(mapId);
    }

    /**
     * Get all maps.
     */
    public Map<Integer, MapData> getAllMaps() {
        return maps;
    }

    /**
     * Get map count.
     */
    public int getMapCount() {
        return maps.size();
    }

    /**
     * Save to JSON.
     */
    public void save() {
        persistence.saveJson(FILENAME, maps);
        log.debug("Saved {} maps", maps.size());
    }

    /**
     * Load from JSON.
     */
    public void load() {
        Type type = new TypeToken<Map<Integer, MapData>>(){}.getType();
        String json = null;
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("data", FILENAME);
            if (java.nio.file.Files.exists(path)) {
                json = java.nio.file.Files.readString(path);
                Map<Integer, MapData> loaded = persistence.getGson().fromJson(json, type);
                if (loaded != null) {
                    maps.clear();
                    maps.putAll(loaded);
                    log.info("Loaded {} maps from {}", maps.size(), FILENAME);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load maps: {}", e.getMessage(), e);
        }
    }
}
