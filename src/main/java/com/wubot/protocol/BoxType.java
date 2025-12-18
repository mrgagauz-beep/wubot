package com.wubot.protocol;

/**
 * Box (Collectable) type constants.
 */
public final class BoxType {
    private BoxType() {}

    public static final int BONUS_BOX = 1;
    public static final int RESOURCE_1 = 2;
    public static final int RESOURCE_2 = 3;
    public static final int LOOT = 4;
    public static final int SPECIAL_1 = 13;
    public static final int SPECIAL_2 = 14;

    /**
     * Check if box type should be collected.
     *
     * @param type box type
     * @param cargoFull true if player cargo is full
     * @return true if should collect
     */
    public static boolean isCollectable(int type, boolean cargoFull) {
        return switch (type) {
            case BONUS_BOX -> true;
            case LOOT -> !cargoFull;
            default -> false;
        };
    }

    /**
     * Check if box type is always collectable (ignoring cargo).
     */
    public static boolean isAlwaysCollectable(int type) {
        return type == BONUS_BOX;
    }

    /**
     * Get human-readable name for box type.
     */
    public static String getName(int type) {
        return switch (type) {
            case BONUS_BOX -> "BONUS_BOX";
            case RESOURCE_1 -> "RESOURCE_1";
            case RESOURCE_2 -> "RESOURCE_2";
            case LOOT -> "LOOT";
            case SPECIAL_1 -> "SPECIAL_1";
            case SPECIAL_2 -> "SPECIAL_2";
            default -> "UNKNOWN(" + type + ")";
        };
    }
}
