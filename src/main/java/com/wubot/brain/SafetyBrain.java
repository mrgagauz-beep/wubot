package com.wubot.brain;

import com.wubot.world.PlayerEntity;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safety logic - determines when to flee and where to go.
 */
public class SafetyBrain {
    private static final Logger log = LoggerFactory.getLogger(SafetyBrain.class);

    /**
     * Check if bot should flee to safety.
     *
     * @param world current world snapshot
     * @return true if should flee
     */
    public boolean shouldFlee(WorldSnapshot world) {
        int hp = world.getPlayerHp();
        int maxHp = world.getPlayerMaxHp();
        double percent = world.playerHpPercent() * 100;
        log.debug("shouldFlee check: hp={}, maxHp={}, percent={:.1f}%, fleeThreshold={:.1f}%",
                  hp, maxHp, percent, BotConfig.FLEE_HP_PERCENT * 100);

        // Skip HP checks if MAX_HP is not initialized yet (would give false 0% reading)
        if (maxHp == 0) {
            log.debug("shouldFlee=FALSE: maxHp not initialized");
            return false;
        }

        // Critical HP - always flee
        if (isCritical(world)) {
            log.info("shouldFlee=TRUE: reason=CRITICAL_HP ({:.1f}% < {:.1f}%)",
                     percent, BotConfig.CRITICAL_HP_PERCENT * 100);
            return true;
        }

        // Low HP - flee
        if (world.playerHpPercent() < BotConfig.FLEE_HP_PERCENT) {
            log.info("shouldFlee=TRUE: reason=LOW_HP ({:.1f}% < {:.1f}%)",
                     percent, BotConfig.FLEE_HP_PERCENT * 100);
            return true;
        }

        // Enemy player nearby - flee
        if (isEnemyPlayerNearby(world, BotConfig.ENEMY_DETECTION_RANGE)) {
            log.info("shouldFlee=TRUE: reason=ENEMY_PLAYER_NEARBY");
            return true;
        }

        log.debug("shouldFlee=FALSE: hp ok ({:.1f}% >= {:.1f}%), no enemies",
                  percent, BotConfig.FLEE_HP_PERCENT * 100);
        return false;
    }

    /**
     * Check if HP is critical.
     *
     * @param world current world snapshot
     * @return true if HP is critically low
     */
    public boolean isCritical(WorldSnapshot world) {
        // Don't report critical if MAX_HP not initialized
        if (world.getPlayerMaxHp() == 0) {
            return false;
        }
        return world.playerHpPercent() < BotConfig.CRITICAL_HP_PERCENT;
    }

    /**
     * Check if any enemy player is nearby.
     *
     * @param world current world snapshot
     * @param radius detection radius
     * @return true if enemy player found within radius
     */
    public boolean isEnemyPlayerNearby(WorldSnapshot world, float radius) {
        for (PlayerEntity player : world.getPlayers()) {
            if (player.isEnemy() && !player.isDead()) {
                float dist = world.distanceTo(player.getX(), player.getY());
                if (dist <= radius) {
                    log.debug("Enemy player {} at distance {}", player.getId(), dist);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if currently in safe zone.
     *
     * @param world current world snapshot
     * @return true if in safe zone
     */
    public boolean isInSafeZone(WorldSnapshot world) {
        log.debug("isInSafeZone check: serverSays={}, playerPos=({},{}), safeZonePos=({},{})",
                  world.isInSafeZone(), world.getPlayerX(), world.getPlayerY(),
                  world.getSafeZoneX(), world.getSafeZoneY());
        return world.isInSafeZone();
    }

    /**
     * Check if HP and Shield are fully restored.
     *
     * @param world current world snapshot
     * @return true if HP and Shield are at max
     */
    public boolean isFullyRepaired(WorldSnapshot world) {
        boolean hpFull = world.getPlayerHp() >= world.getPlayerMaxHp();

        // Check shield only if maxShield is initialized (> 0)
        boolean shieldFull = world.getPlayerMaxShield() <= 0
            || world.getPlayerShield() >= world.getPlayerMaxShield();

        return hpFull && shieldFull;
    }

    /**
     * Get distance to safe zone center.
     *
     * @param world current world snapshot
     * @return distance to safe zone
     */
    public float getDistanceToSafeZone(WorldSnapshot world) {
        return world.distanceToSafeZone();
    }
}
