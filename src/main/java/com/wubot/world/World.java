package com.wubot.world;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main world state container.
 * Thread-safe storage for all game entities.
 */
public class World {
    private final PlayerState player;
    private final MapInfo mapInfo;
    private final Map<Integer, NpcEntity> npcs;
    private final Map<Integer, PlayerEntity> players;
    private final Map<Integer, BoxEntity> boxes;

    private volatile int currentTargetId = -1;
    private volatile boolean attacking = false;

    public World() {
        this.player = new PlayerState();
        this.mapInfo = new MapInfo();
        this.npcs = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.boxes = new ConcurrentHashMap<>();
    }

    // === Entity Access ===

    public PlayerState getPlayer() { return player; }
    public MapInfo getMapInfo() { return mapInfo; }

    // === NPCs ===

    public NpcEntity getNpc(int id) {
        return npcs.get(id);
    }

    public NpcEntity getOrCreateNpc(int id) {
        return npcs.computeIfAbsent(id, NpcEntity::new);
    }

    public void removeNpc(int id) {
        npcs.remove(id);
    }

    public Collection<NpcEntity> getAllNpcs() {
        return npcs.values();
    }

    public void clearNpcs() {
        npcs.clear();
    }

    // === Players ===

    public PlayerEntity getPlayerEntity(int id) {
        return players.get(id);
    }

    public PlayerEntity getOrCreatePlayer(int id) {
        return players.computeIfAbsent(id, PlayerEntity::new);
    }

    public void removePlayer(int id) {
        players.remove(id);
    }

    public Collection<PlayerEntity> getAllPlayers() {
        return players.values();
    }

    public void clearPlayers() {
        players.clear();
    }

    // === Boxes ===

    public BoxEntity getBox(int id) {
        return boxes.get(id);
    }

    public BoxEntity getOrCreateBox(int id) {
        return boxes.computeIfAbsent(id, BoxEntity::new);
    }

    public void removeBox(int id) {
        boxes.remove(id);
    }

    public Collection<BoxEntity> getAllBoxes() {
        return boxes.values();
    }

    public void clearBoxes() {
        boxes.clear();
    }

    // === Combat State ===

    public int getCurrentTargetId() { return currentTargetId; }
    public void setCurrentTargetId(int targetId) { this.currentTargetId = targetId; }

    public boolean isAttacking() { return attacking; }
    public void setAttacking(boolean attacking) { this.attacking = attacking; }

    // === Snapshot ===

    /**
     * Create immutable snapshot of current world state.
     */
    public WorldSnapshot snapshot() {
        return new WorldSnapshot(this);
    }

    // === Utility ===

    /**
     * Clear all entities (on map change).
     */
    public void clearAll() {
        npcs.clear();
        players.clear();
        boxes.clear();
        currentTargetId = -1;
        attacking = false;
    }

    @Override
    public String toString() {
        return String.format("World{player=%s, map=%s, npcs=%d, players=%d, boxes=%d}",
                player, mapInfo, npcs.size(), players.size(), boxes.size());
    }
}
