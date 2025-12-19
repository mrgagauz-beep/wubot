package com.wubot.action;

/**
 * Sealed interface representing bot actions.
 * Used by Brain to express intentions, executed by ActionExecutor.
 */
public sealed interface Action {

    /**
     * Move to position.
     */
    record Move(float x, float y) implements Action {}

    /**
     * Lock target (NPC or player).
     */
    record Lock(int targetId) implements Action {}

    /**
     * Start attacking locked target.
     */
    record Attack() implements Action {}

    /**
     * Stop attacking.
     */
    record StopAttack() implements Action {}

    /**
     * Collect a box.
     * boxX, boxY are the box coordinates (Y_OFFSET will be applied by executor).
     */
    record Collect(int boxId, float boxX, float boxY) implements Action {}

    /**
     * Use teleport portal.
     * portalIndex is the array index in teleports[] for discovery tracking.
     * portalId is the ID from TPort.id for the TeleportRequestPacket.
     */
    record UseTeleport(int portalIndex, int portalId) implements Action {}

    /**
     * Switch ship configuration (1 or 2).
     */
    record SwitchConfig(int configId) implements Action {}

    /**
     * Switch to another ship.
     */
    record SwitchShip(int shipId) implements Action {}

    /**
     * Wait for specified duration (used for delays, e.g., after move before collect).
     */
    record Wait(long durationMs) implements Action {}
}
