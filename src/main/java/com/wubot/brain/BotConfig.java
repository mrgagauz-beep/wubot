package com.wubot.brain;

/**
 * Bot configuration constants.
 */
public final class BotConfig {
    private BotConfig() {}

    // === Safety thresholds ===

    /** HP percentage to trigger flee */
    public static final float FLEE_HP_PERCENT = 0.5f;

    /** HP percentage for critical state */
    public static final float CRITICAL_HP_PERCENT = 0.15f;

    /** Detection range for enemy players */
    public static final float ENEMY_DETECTION_RANGE = 2000f;

    // === Kite distances (orbit distance from target) ===

    /** Kite distance for boss NPCs (HP >= 500k) */
    public static final float KITE_DISTANCE_BOSS = 850f;

    /** Kite distance for medium NPCs (HP >= 70k) */
    public static final float KITE_DISTANCE_MEDIUM = 800f;

    /** Kite distance for weak NPCs (HP < 70k) */
    public static final float KITE_DISTANCE_WEAK = 750f;

    /** HP threshold for boss classification */
    public static final int BOSS_HP_THRESHOLD = 500_000;

    /** HP threshold for medium classification */
    public static final int MEDIUM_HP_THRESHOLD = 70_000;

    // === Collection ===

    /** Radius to collect boxes during combat */
    public static final float COLLECT_RADIUS = 800f;

    /** Y offset for box collection point */
    public static final float Y_OFFSET = 97f;

    // === Evacuation ===

    /** Distance to portal to trigger teleport request */
    public static final float PORTAL_JUMP_DISTANCE = 100f;

    /** Distance to fly beyond portal (not stop at it) */
    public static final float FLEE_BEYOND_PORTAL = 1000f;

    /** Angle for path blocking check (cos 60°) */
    public static final float PATH_BLOCK_ANGLE = 0.5f;

    // === Tick rate ===

    /** Game loop tick interval in milliseconds */
    public static final int TICK_INTERVAL_MS = 100;

    /** Minimum interval between packets */
    public static final int PACKET_MIN_INTERVAL_MS = 50;

    // === Utility methods ===

    /**
     * Get kite distance based on NPC max HP.
     */
    public static float getKiteDistance(int maxHp) {
        if (maxHp >= BOSS_HP_THRESHOLD) {
            return KITE_DISTANCE_BOSS;
        } else if (maxHp >= MEDIUM_HP_THRESHOLD) {
            return KITE_DISTANCE_MEDIUM;
        } else {
            return KITE_DISTANCE_WEAK;
        }
    }
}
