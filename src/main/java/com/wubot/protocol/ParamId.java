package com.wubot.protocol;

/**
 * Parameter IDs used in ChangedParameter.
 * These IDs identify what value is being updated in Ship/Player state.
 */
public final class ParamId {
    private ParamId() {}

    // Entity identification
    public static final int NPC_TYPE = 9;
    public static final int ENTITY_TYPE = 42;  // 3 = NPC, 100 = Player
    public static final int NAME = 12;  // Entity name

    // Position and movement
    public static final int POSITION = 17;
    public static final int SPEED = 37;  // Verified from packet analysis (NPC speed as float)

    // Health (NPC)
    // Note: ParamId 24/31 appear to be scaled/UI values (e.g., 110/200)
    // ParamId 34 contains the real absolute HP value (e.g., 2000)
    public static final int HP = 24;          // NPC HP (scaled)
    public static final int MAX_HP = 31;      // NPC Max HP (scaled)
    public static final int NPC_REAL_HP = 34; // NPC real absolute HP (e.g., 2000 for weak NPCs)

    // Health (Player)
    public static final int PLAYER_HP = 25;
    public static final int PLAYER_MAX_HP = 26;

    // Shield (both Player and NPC use same IDs)
    public static final int SHIELD = 27;           // Player Shield
    public static final int MAX_SHIELD = 28;       // Player Max Shield
    public static final int PLAYER_SHIELD = 27;    // Alias for clarity
    public static final int PLAYER_MAX_SHIELD = 28; // Alias for clarity

    // Cargo
    public static final int CARGO_USED = 32;
    public static final int CARGO_MAX = 33;

    // Target (for locked target info)
    public static final int PARAM_TARGET_ID = 20;
}
