package com.wubot.world;

/**
 * Current map information.
 * Updated from MapInfoPacket.
 */
public class MapInfo {
    private int mapId;
    private String mapName;
    private float width, height;
    private float safeZoneX, safeZoneY;
    private float safeZoneRadius;

    // === Getters ===

    public int getMapId() { return mapId; }
    public String getMapName() { return mapName; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getSafeZoneX() { return safeZoneX; }
    public float getSafeZoneY() { return safeZoneY; }
    public float getSafeZoneRadius() { return safeZoneRadius; }

    // === Setters ===

    public void setMapId(int mapId) { this.mapId = mapId; }
    public void setMapName(String mapName) { this.mapName = mapName; }
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }
    public void setSafeZone(float x, float y, float radius) {
        this.safeZoneX = x;
        this.safeZoneY = y;
        this.safeZoneRadius = radius;
    }

    // === Computed ===

    /**
     * Check if position is within map bounds.
     */
    public boolean isInBounds(float x, float y) {
        return x >= 0 && x <= width && y >= 0 && y <= height;
    }

    /**
     * Check if position is in safe zone.
     */
    public boolean isInSafeZone(float x, float y) {
        if (safeZoneRadius <= 0) return false;
        float dx = x - safeZoneX;
        float dy = y - safeZoneY;
        return Math.sqrt(dx * dx + dy * dy) <= safeZoneRadius;
    }

    /**
     * Get distance to safe zone center.
     */
    public float distanceToSafeZone(float x, float y) {
        float dx = safeZoneX - x;
        float dy = safeZoneY - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Clamp position to map bounds.
     */
    public float[] clampToBounds(float x, float y) {
        float clampedX = Math.max(0, Math.min(width, x));
        float clampedY = Math.max(0, Math.min(height, y));
        return new float[]{clampedX, clampedY};
    }

    @Override
    public String toString() {
        return String.format("Map{id=%d, name='%s', size=(%.0f,%.0f), safeZone=(%.0f,%.0f,r=%.0f)}",
                mapId, mapName, width, height, safeZoneX, safeZoneY, safeZoneRadius);
    }
}
