package com.wubot.brain;

/**
 * Bot FSM states.
 */
public enum BotState {
    /** Waiting, no active task */
    IDLE,

    /** Farming NPCs and collecting loot */
    FARMING,

    /** Only collecting boxes (no combat) */
    COLLECTING,

    /** Fleeing from danger to safety */
    FLEEING,

    /** Repairing in safe zone */
    REPAIRING,

    /** Moving between maps via portals */
    NAVIGATING,

    /** Switching to another ship */
    SWITCHING_SHIP,

    /** Exploring map for discovery */
    EXPLORING
}
