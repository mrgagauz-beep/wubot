package com.wubot.brain;

import com.wubot.action.Action;
import com.wubot.protocol.BoxType;
import com.wubot.world.BoxEntity;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection logic - finding and collecting boxes.
 * Handles the MOVE → WAIT → COLLECT sequence properly.
 */
public class CollectBrain {
    private static final Logger log = LoggerFactory.getLogger(CollectBrain.class);

    /** Minimum time to wait after move before collecting (ms) */
    private static final long COLLECT_WAIT_MS = 800;

    /** Minimum distance to box to consider "arrived" */
    private static final float ARRIVAL_DISTANCE = 50f;

    private Integer collectingBoxId = null;
    private long collectStartTime = 0;
    private boolean waitingForArrival = false;
    private float targetX, targetY;

    /**
     * Get collection actions for current tick.
     *
     * @param world current world snapshot
     * @return list of actions
     */
    public List<Action> getActions(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        // If cargo is full, only collect BONUS_BOX
        boolean cargoFull = world.isCargoFull();

        // Already collecting a box - check if we can pick it up
        if (collectingBoxId != null && waitingForArrival) {
            BoxEntity box = world.findBox(collectingBoxId);

            // Box no longer exists
            if (box == null) {
                log.debug("Box {} disappeared during collection", collectingBoxId);
                resetCollection();
                return actions;
            }

            // Check if we've arrived at collection point
            float distToTarget = world.distanceTo(targetX, targetY);
            long elapsed = System.currentTimeMillis() - collectStartTime;

            if (distToTarget < ARRIVAL_DISTANCE || elapsed >= COLLECT_WAIT_MS) {
                // Arrived or waited long enough - collect!
                log.debug("Collecting box {} (dist={}, waited={}ms)", collectingBoxId, distToTarget, elapsed);
                actions.add(new Action.Collect(collectingBoxId, box.getX(), box.getY()));
                resetCollection();
            }

            return actions;
        }

        // Not currently collecting - find a box to collect
        BoxEntity box = findBestBox(world, BotConfig.COLLECT_RADIUS, cargoFull);
        if (box != null) {
            // Start collection - move to box position with Y_OFFSET
            collectingBoxId = box.getId();
            collectStartTime = System.currentTimeMillis();
            waitingForArrival = true;
            targetX = box.getX();
            targetY = box.getY() + BotConfig.Y_OFFSET;

            log.debug("Moving to collect box {} at ({}, {})", box.getId(), targetX, targetY);
            actions.add(new Action.Move(targetX, targetY));
        }

        return actions;
    }

    /**
     * Find best box to collect within radius.
     * Priority: closer to player, BONUS_BOX > LOOT
     */
    public BoxEntity findBestBox(WorldSnapshot world, float maxRadius, boolean cargoFull) {
        BoxEntity bestBox = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (BoxEntity box : world.getBoxes()) {
            // Check if we should collect this box type
            if (!BoxType.isCollectable(box.getBoxType(), cargoFull)) {
                continue;
            }

            // Check distance
            float dist = world.distanceTo(box.getX(), box.getY());
            if (dist > maxRadius) {
                continue;
            }

            // Score: closer = better, BONUS_BOX > LOOT
            float score = 1000f - dist;
            if (box.getBoxType() == BoxType.BONUS_BOX) {
                score += 500f; // Priority for bonus boxes
            }

            if (score > bestScore) {
                bestScore = score;
                bestBox = box;
            }
        }

        return bestBox;
    }

    /**
     * Reset collection state.
     */
    private void resetCollection() {
        collectingBoxId = null;
        collectStartTime = 0;
        waitingForArrival = false;
    }

    /**
     * Check if currently collecting.
     */
    public boolean isCollecting() {
        return collectingBoxId != null;
    }

    /**
     * Cancel current collection.
     */
    public void cancelCollection() {
        if (collectingBoxId != null) {
            log.debug("Collection cancelled for box {}", collectingBoxId);
            resetCollection();
        }
    }
}
