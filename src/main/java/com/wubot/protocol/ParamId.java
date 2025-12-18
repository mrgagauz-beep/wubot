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

    // Health - REAL HP values (both NPC and Player use ParamId 25/26)
    // Discovered from packet analysis: Hydro NPC has ParamId 25=800, ParamId 26=800
    // This matches documentation "Hydro ~800-1000 HP"
    public static final int HP = 25;          // Real HP (both NPC and Player)
    public static final int MAX_HP = 26;      // Real Max HP (both NPC and Player)
    
    // UI Scale values (NOT real HP - do not use for HP tracking)
    // ParamId 24/31 are scaled UI values (e.g., 110/200 for Hydro)
    // ParamId 34 is unknown (e.g., 2000) - NOT HP
    public static final int UI_HP_SCALE = 24;      // UI scale value (not real HP)
    public static final int UI_MAX_HP_SCALE = 31;  // UI scale value (not real HP)
    public static final int UNKNOWN_34 = 34;       // Unknown value (not HP)

    // Aliases for clarity (deprecated - use HP/MAX_HP instead)
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
