package com.wubot.brain;

import com.wubot.action.Action;
import com.wubot.world.BoxEntity;
import com.wubot.world.NpcEntity;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat logic - target selection, kiting, attacking.
 */
public class CombatBrain {
    private static final Logger log = LoggerFactory.getLogger(CombatBrain.class);

    private Integer currentTargetId = null;
    private long lastAttackTime = 0;
    private float orbitAngle = 0;

    // Lock state tracking
    private boolean lockSent = false;
    private boolean lockConfirmed = false;
    private long lockSentTime = 0;
    
    // Attack confirmation tracking
    private int lastKnownTargetHp = -1;
    private int lastKnownTargetShield = -1;
    private long lastDamageTime = 0;
    private int totalDamageDealt = 0;

    /**
     * Get combat actions for current tick.
     * State machine: IDLE → SELECT_TARGET → LOCK → WAIT_FOR_HP → ATTACK
     *
     * @param world current world snapshot
     * @return list of actions
     */
    public List<Action> getActions(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        // Check if we need a new target
        if (needsNewTarget(world)) {
            NpcEntity target = selectTarget(world);
            if (target != null) {
                currentTargetId = target.getId();
                lockSent = false;
                lockConfirmed = false;
                log.info("New target selected: {}", target);
            }
            return actions;
        }

        // Have a target - execute combat state machine
        NpcEntity target = world.findNpc(currentTargetId);
        if (target == null) {
            log.warn("Target {} disappeared!", currentTargetId);
            clearTarget();
            return actions;
        }

        // STATE 1: Send LOCK if not sent yet
        if (!lockSent) {
            log.info("Sending LOCK to target: {}", target);
            actions.add(new Action.Lock(target.getId()));
            lockSent = true;
            lockSentTime = System.currentTimeMillis();
            return actions;
        }

        // STATE 2: Wait for HP to appear (lock confirmation)
        if (!lockConfirmed) {
            // Check if we received HP data (maxHp > 0 means lock confirmed)
            if (target.getMaxHp() > 0) {
                log.info("Lock confirmed! Target HP: {}/{}", target.getHp(), target.getMaxHp());
                lockConfirmed = true;

                // Check if target is already dead
                if (target.isDead()) {
                    log.info("Target was already dead!");
                    clearTarget();
                    return actions;
                }
            } else {
                // Still waiting for lock confirmation
                long waitTime = System.currentTimeMillis() - lockSentTime;
                if (waitTime > 5000) {
                    log.warn("Lock timeout after {}ms, clearing target", waitTime);
                    clearTarget();
                }
                log.trace("Waiting for lock confirmation... ({}ms)", waitTime);
                return actions;
            }
        }

        // STATE 3: APPROACH & ATTACK - we have confirmed HP
        if (lockConfirmed && !target.isDead()) {
            // Track damage confirmation using REAL HP (ParamId 25/26)
            int currentHp = target.getHp();
            int currentShield = target.getShield();
            
            // Calculate distance to target
            float distanceToTarget = world.distanceTo(target.getX(), target.getY());
            
            // Log current HP status - ParamId 25/26 for HP, ParamId 27/28 for Shield
            log.info("[TARGET STATUS] NPC {} - HP={}/{} Shield={}/{} Distance={}px", 
                target.getId(), currentHp, target.getMaxHp(), 
                currentShield, target.getMaxShield(), (int) distanceToTarget);
            
            if (lastKnownTargetHp >= 0) {
                int hpDamage = lastKnownTargetHp - currentHp;
                int shieldDamage = lastKnownTargetShield - currentShield;
                int totalDamage = Math.max(0, hpDamage) + Math.max(0, shieldDamage);
                
                if (totalDamage > 0) {
                    totalDamageDealt += totalDamage;
                    lastDamageTime = System.currentTimeMillis();
                    log.info("[COMBAT] Damage confirmed: {} (scaledHp:{} shield:{}) totalScaledDamage={}", 
                        totalDamage, hpDamage, shieldDamage, totalDamageDealt);
                }
            }
            lastKnownTargetHp = currentHp;
            lastKnownTargetShield = currentShield;
            
            long now = System.currentTimeMillis();
            
            // STATE 3a: APPROACH - fly to target if too far
            if (distanceToTarget > BotConfig.ATTACK_RANGE) {
                log.info("[COMBAT] Too far to attack ({}px > {}px), approaching target...", 
                    (int) distanceToTarget, (int) BotConfig.ATTACK_RANGE);
                // Move directly towards target
                actions.add(new Action.Move(target.getX(), target.getY()));
                return actions;
            }
            
            // STATE 3b: ATTACK - we are in range
            // Warn if no damage for too long (possible attack failure)
            if (lastDamageTime > 0 && now - lastDamageTime > 10000) {
                log.warn("[COMBAT] No damage dealt for {}ms - attack may not be working!", 
                    now - lastDamageTime);
            }
            
            // Attack periodically (send attack command every second to ensure it's active)
            if (now - lastAttackTime > 1000) {
                log.info("[COMBAT] Attacking target {} at distance {}px", target.getId(), (int) distanceToTarget);
                actions.add(new Action.Attack());
                lastAttackTime = now;
            }

            // Calculate kite position and move (stay within attack range!)
            float kiteDistance = getKiteDistance(target);
            float[] kitePos = calculateKitePosition(world, target, kiteDistance);
            actions.add(new Action.Move(kitePos[0], kitePos[1]));

            // Update orbit angle for next tick
            orbitAngle += 2f;
            if (orbitAngle >= 360f) {
                orbitAngle -= 360f;
            }
        }

        return actions;
    }

    /**
     * Check if we need to select a new target.
     */
    public boolean needsNewTarget(WorldSnapshot world) {
        if (currentTargetId == null) {
            return true;
        }

        NpcEntity target = world.findNpc(currentTargetId);
        if (target == null || target.isDead()) {
            log.debug("Current target {} is dead or gone", currentTargetId);
            currentTargetId = null;
            return true;
        }

        return false;
    }

    /**
     * Select best target NPC.
     * IMPORTANT: HP is NOT visible until after LOCK, so we select by distance and type only!
     * Scoring: closer = better, more boxes nearby = better
     * 
     * FILTER: Only select entities with npcType >= 3 (confirmed NPCs).
     * ENTITY_TYPE=2 are other objects (drones, etc.) that should not be targeted.
     */
    public NpcEntity selectTarget(WorldSnapshot world) {
        NpcEntity bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (NpcEntity npc : world.getNpcs()) {
            // CRITICAL: Only target confirmed NPCs (npcType > 0)
            // npcType=0 means entity was not confirmed as NPC (ENTITY_TYPE != 3)
            // npcType=1 = Hydro, npcType=2 = Hyper, npcType=3 = generic NPC
            if (npc.getNpcType() <= 0) {
                log.trace("Skipping non-NPC entity: id={} npcType={}", npc.getId(), npc.getNpcType());
                continue;
            }
            
            // Do NOT filter by isDead() - hp=0/0 before lock is normal!
            // Only skip if we confirmed it's dead (maxHp > 0 && hp = 0)
            if (npc.getMaxHp() > 0 && npc.getHp() <= 0) {
                log.trace("Skipping confirmed dead NPC: {}", npc);
                continue;
            }

            float score = scoreTarget(npc, world);
            log.debug("Target candidate: id={} npcType={} at distance {}, score: {}",
                     npc.getId(), npc.getNpcType(), world.distanceTo(npc.getX(), npc.getY()), score);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = npc;
            }
        }

        if (bestTarget != null) {
            log.info("Selected target: id={} npcType={} at distance {}",
                    bestTarget.getId(), bestTarget.getNpcType(), 
                    world.distanceTo(bestTarget.getX(), bestTarget.getY()));
        } else {
            log.debug("No valid NPC targets found (need npcType >= 3)");
        }

        return bestTarget;
    }

    /**
     * Calculate target score.
     * IMPORTANT: Do NOT use HP - it's 0 before lock!
     */
    private float scoreTarget(NpcEntity npc, WorldSnapshot world) {
        float score = 1000f;

        // Distance penalty (closer = better)
        float dist = world.distanceTo(npc.getX(), npc.getY());
        score -= dist * 0.1f;

        // DO NOT USE HP - it's not available until after lock!
        // score -= npc.getHp() * 0.001f;  // REMOVED

        // Bonus for boxes nearby (more loot = better)
        int nearbyBoxes = countBoxesNear(npc, world, 500f);
        score += nearbyBoxes * 100f;

        // NPC type bonus (prefer certain types if needed)
        // TODO: Add NPC type preferences when we identify valuable NPCs

        return score;
    }

    /**
     * Count boxes near an NPC.
     */
    private int countBoxesNear(NpcEntity npc, WorldSnapshot world, float radius) {
        int count = 0;
        for (BoxEntity box : world.getBoxes()) {
            float dx = box.getX() - npc.getX();
            float dy = box.getY() - npc.getY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= radius) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get kite distance based on NPC HP.
     */
    public float getKiteDistance(NpcEntity npc) {
        return BotConfig.getKiteDistance(npc.getMaxHp());
    }

    /**
     * Calculate kite position (orbit around target).
     */
    public float[] calculateKitePosition(WorldSnapshot world, NpcEntity target, float kiteDistance) {
        // Calculate position on orbit around target
        float angleRad = (float) Math.toRadians(orbitAngle);
        float targetX = target.getX() + kiteDistance * (float) Math.cos(angleRad);
        float targetY = target.getY() + kiteDistance * (float) Math.sin(angleRad);

        return new float[]{targetX, targetY};
    }

    /**
     * Get current target ID.
     */
    public Integer getCurrentTargetId() {
        return currentTargetId;
    }

    /**
     * Clear current target.
     */
    public void clearTarget() {
        currentTargetId = null;
        lockSent = false;
        lockConfirmed = false;
        lockSentTime = 0;
        lastKnownTargetHp = -1;
        lastKnownTargetShield = -1;
        lastDamageTime = 0;
        totalDamageDealt = 0;
    }
    
    /**
     * Get total damage dealt to current target.
     */
    public int getTotalDamageDealt() {
        return totalDamageDealt;
    }
}
