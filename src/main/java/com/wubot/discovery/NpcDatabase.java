package com.wubot.discovery;

import com.google.gson.reflect.TypeToken;
import com.wubot.persistence.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database of discovered NPC types and their statistics.
 */
public class NpcDatabase {
    private static final Logger log = LoggerFactory.getLogger(NpcDatabase.class);

    private static final String FILENAME = "npcs.json";

    private final PersistenceManager persistence;

    /** NPC type data indexed by npcType */
    private final Map<Integer, NpcTypeData> npcTypes;

    /** NPCs seen on each map */
    private final Map<Integer, Set<Integer>> npcsByMap;

    /**
     * NPC type statistics holder.
     */
    public static class NpcTypeData {
        public int npcType;
        public int minHp;
        public int maxHp;
        public long totalHp;
        public float speed;
        public Set<Integer> seenOnMaps;
        public int killCount;
        public int sampleCount;

        public NpcTypeData() {
            this.seenOnMaps = new HashSet<>();
        }

        public NpcTypeData(int npcType) {
            this.npcType = npcType;
            this.minHp = Integer.MAX_VALUE;
            this.maxHp = 0;
            this.totalHp = 0;
            this.seenOnMaps = new HashSet<>();
            this.killCount = 0;
            this.sampleCount = 0;
        }

        /**
         * Get average HP.
         */
        public int getAvgHp() {
            return sampleCount > 0 ? (int) (totalHp / sampleCount) : 0;
        }
    }

    /**
     * Serialization wrapper.
     */
    public static class NpcDatabaseData {
        public Map<Integer, NpcTypeData> npcTypes;
        public Map<Integer, Set<Integer>> npcsByMap;
    }

    public NpcDatabase(PersistenceManager persistence) {
        this.persistence = persistence;
        this.npcTypes = new ConcurrentHashMap<>();
        this.npcsByMap = new ConcurrentHashMap<>();
    }

    /**
     * Discover/update NPC type from spawn.
     *
     * @param npcType NPC type ID
     * @param mapId map where NPC was seen
     * @param hp current HP
     * @param maxHp maximum HP
     * @param speed NPC speed
     * @return true if this was a NEW NPC type or NEW map for existing type
     */
    public boolean discover(int npcType, int mapId, int hp, int maxHp, float speed) {
        NpcTypeData data = npcTypes.computeIfAbsent(npcType, NpcTypeData::new);
        boolean isNew = data.sampleCount == 0;

        // Update statistics
        if (maxHp > 0) {
            data.minHp = Math.min(data.minHp, maxHp);
            data.maxHp = Math.max(data.maxHp, maxHp);
            data.totalHp += maxHp;
            data.sampleCount++;
        }

        if (speed > 0) {
            data.speed = speed;
        }

        // Track map
        boolean newMap = data.seenOnMaps.add(mapId);

        // Track NPC on map
        npcsByMap.computeIfAbsent(mapId, k -> ConcurrentHashMap.newKeySet()).add(npcType);

        if (isNew) {
            log.info("👾 NEW NPC TYPE: {} (maxHp={}, speed={})", npcType, maxHp, speed);
            return true;
        } else if (newMap) {
            log.debug("NPC type {} now seen on map {}", npcType, mapId);
            return true;
        }
        return false;
    }

    /**
     * Record NPC kill.
     */
    public void recordKill(int npcType) {
        NpcTypeData data = npcTypes.get(npcType);
        if (data != null) {
            data.killCount++;
            log.trace("Kill recorded for NPC type {}, total kills: {}", npcType, data.killCount);
        }
    }

    /**
     * Get NPC type data.
     */
    public NpcTypeData getNpcType(int npcType) {
        return npcTypes.get(npcType);
    }

    /**
     * Get all NPC types.
     */
    public Map<Integer, NpcTypeData> getAllNpcTypes() {
        return npcTypes;
    }

    /**
     * Get NPC types seen on a specific map.
     */
    public Set<Integer> getNpcTypesOnMap(int mapId) {
        return npcsByMap.getOrDefault(mapId, Collections.emptySet());
    }

    /**
     * Get maps where an NPC type has been seen.
     */
    public Set<Integer> getMapsWithNpc(int npcType) {
        NpcTypeData data = npcTypes.get(npcType);
        return data != null ? data.seenOnMaps : Collections.emptySet();
    }

    /**
     * Get NPC type count.
     */
    public int getNpcTypeCount() {
        return npcTypes.size();
    }

    /**
     * Estimate difficulty of NPC type (based on HP).
     */
    public float estimateDifficulty(int npcType) {
        NpcTypeData data = npcTypes.get(npcType);
        if (data == null || data.sampleCount == 0) {
            return 0f;
        }
        return data.getAvgHp();
    }

    /**
     * Save to JSON.
     */
    public void save() {
        NpcDatabaseData data = new NpcDatabaseData();
        data.npcTypes = new HashMap<>(npcTypes);
        data.npcsByMap = new HashMap<>(npcsByMap);
        persistence.saveJson(FILENAME, data);
        log.debug("Saved {} NPC types", npcTypes.size());
    }

    /**
     * Load from JSON.
     */
    public void load() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("data", FILENAME);
            if (java.nio.file.Files.exists(path)) {
                String json = java.nio.file.Files.readString(path);
                NpcDatabaseData data = persistence.getGson().fromJson(json, NpcDatabaseData.class);
                if (data != null) {
                    npcTypes.clear();
                    npcsByMap.clear();
                    if (data.npcTypes != null) {
                        npcTypes.putAll(data.npcTypes);
                    }
                    if (data.npcsByMap != null) {
                        npcsByMap.putAll(data.npcsByMap);
                    }
                    log.info("Loaded {} NPC types from {}", npcTypes.size(), FILENAME);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load NPCs: {}", e.getMessage(), e);
        }
    }
}
