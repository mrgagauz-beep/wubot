package com.wubot.world;

/**
 * Collectable box entity.
 * Updated from CollectableInPacket.
 *
 * Box types:
 * - 1 = BONUS_BOX (always collect)
 * - 4 = LOOT (collect if cargo not full)
 * - 2, 3, 13, 14 = ignore
 */
public class BoxEntity {
    public static final int TYPE_BONUS_BOX = 1;
    public static final int TYPE_LOOT = 4;

    /** Y offset for collection point */
    public static final float Y_OFFSET = 97f;

    private final int id;
    private int boxType;
    private float x, y;
    private long spawnTime;

    public BoxEntity(int id) {
        this.id = id;
        this.spawnTime = System.currentTimeMillis();
    }

    // === Getters ===

    public int getId() { return id; }
    public int getBoxType() { return boxType; }
    public float getX() { return x; }
    public float getY() { return y; }
    public long getSpawnTime() { return spawnTime; }

    // === Setters ===

    public void setBoxType(int boxType) { this.boxType = boxType; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // === Computed ===

    /**
     * Check if this box should be collected.
     *
     * @param cargoFull true if player cargo is full
     * @return true if box should be collected
     */
    public boolean shouldCollect(boolean cargoFull) {
        return switch (boxType) {
            case TYPE_BONUS_BOX -> true;
            case TYPE_LOOT -> !cargoFull;
            default -> false;
        };
    }

    /**
     * Get collection point Y coordinate (with offset).
     */
    public float getCollectY() {
        return y + Y_OFFSET;
    }

    public float distanceTo(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Get distance to collection point.
     */
    public float distanceToCollectPoint(float playerX, float playerY) {
        float dx = x - playerX;
        float dy = getCollectY() - playerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("Box{id=%d, type=%d, pos=(%.0f,%.0f)}",
                id, boxType, x, y);
    }
}
