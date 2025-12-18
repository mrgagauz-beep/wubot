package com.wubot.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of world state for brain decision making.
 * Created from World at each tick.
 */
public class WorldSnapshot {
    private final float playerX, playerY;
    private final int playerHp, playerMaxHp;
    private final int playerShield, playerMaxShield;
    private final float playerSpeed;
    private final int cargoUsed, cargoMax;
    private final boolean inSafeZone;
    private final int configId;

    private final int mapId;
    private final float mapWidth, mapHeight;
    private final float safeZoneX, safeZoneY, safeZoneRadius;

    private final List<NpcEntity> npcs;
    private final List<PlayerEntity> players;
    private final List<BoxEntity> boxes;

    private final int currentTargetId;
    private final boolean attacking;

    public WorldSnapshot(World world) {
        PlayerState player = world.getPlayer();
        this.playerX = player.getX();
        this.playerY = player.getY();
        this.playerHp = player.getHp();
        this.playerMaxHp = player.getMaxHp();
        this.playerShield = player.getShield();
        this.playerMaxShield = player.getMaxShield();
        this.playerSpeed = player.getSpeed();
        this.cargoUsed = player.getCargoUsed();
        this.cargoMax = player.getCargoMax();
        this.inSafeZone = player.isInSafeZone();
        this.configId = player.getConfigId();

        MapInfo map = world.getMapInfo();
        this.mapId = map.getMapId();
        this.mapWidth = map.getWidth();
        this.mapHeight = map.getHeight();
        this.safeZoneX = map.getSafeZoneX();
        this.safeZoneY = map.getSafeZoneY();
        this.safeZoneRadius = map.getSafeZoneRadius();

        this.npcs = Collections.unmodifiableList(new ArrayList<>(world.getAllNpcs()));
        this.players = Collections.unmodifiableList(new ArrayList<>(world.getAllPlayers()));
        this.boxes = Collections.unmodifiableList(new ArrayList<>(world.getAllBoxes()));

        this.currentTargetId = world.getCurrentTargetId();
        this.attacking = world.isAttacking();
    }

    // === Player State ===

    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }
    public int getPlayerHp() { return playerHp; }
    public int getPlayerMaxHp() { return playerMaxHp; }
    public int getPlayerShield() { return playerShield; }
    public int getPlayerMaxShield() { return playerMaxShield; }
    public float getPlayerSpeed() { return playerSpeed; }
    public int getCargoUsed() { return cargoUsed; }
    public int getCargoMax() { return cargoMax; }
    public boolean isInSafeZone() { return inSafeZone; }
    public int getConfigId() { return configId; }

    // === Map Info ===

    public int getMapId() { return mapId; }
    public float getMapWidth() { return mapWidth; }
    public float getMapHeight() { return mapHeight; }
    public float getSafeZoneX() { return safeZoneX; }
    public float getSafeZoneY() { return safeZoneY; }
    public float getSafeZoneRadius() { return safeZoneRadius; }

    // === Entities ===

    public List<NpcEntity> getNpcs() { return npcs; }
    public List<PlayerEntity> getPlayers() { return players; }
    public List<BoxEntity> getBoxes() { return boxes; }

    // === Combat State ===

    public int getCurrentTargetId() { return currentTargetId; }
    public boolean isAttacking() { return attacking; }

    // === Computed ===

    public float playerHpPercent() {
        return playerMaxHp > 0 ? (float) playerHp / playerMaxHp : 0;
    }

    public float playerShieldPercent() {
        return playerMaxShield > 0 ? (float) playerShield / playerMaxShield : 0;
    }

    public boolean isCargoFull() {
        return cargoMax > 0 && cargoUsed >= cargoMax;
    }

    public boolean isHpCritical(float threshold) {
        return playerHpPercent() < threshold;
    }

    public float distanceTo(float x, float y) {
        float dx = x - playerX;
        float dy = y - playerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float distanceToSafeZone() {
        float dx = safeZoneX - playerX;
        float dy = safeZoneY - playerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Check if any enemy players are nearby.
     */
    public boolean hasEnemyPlayersNearby(float range) {
        for (PlayerEntity p : players) {
            if (p.isEnemy() && !p.isDead()) {
                float dist = distanceTo(p.getX(), p.getY());
                if (dist <= range) return true;
            }
        }
        return false;
    }

    /**
     * Find NPC by ID.
     */
    public NpcEntity findNpc(int id) {
        for (NpcEntity npc : npcs) {
            if (npc.getId() == id) return npc;
        }
        return null;
    }

    /**
     * Find box by ID.
     */
    public BoxEntity findBox(int id) {
        for (BoxEntity box : boxes) {
            if (box.getId() == id) return box;
        }
        return null;
    }

    /**
     * Get current target NPC (if exists and alive).
     */
    public NpcEntity getCurrentTarget() {
        if (currentTargetId < 0) return null;
        NpcEntity target = findNpc(currentTargetId);
        if (target != null && !target.isDead()) return target;
        return null;
    }
}
