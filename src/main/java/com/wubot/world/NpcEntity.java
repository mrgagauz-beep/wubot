package com.wubot.world;

/**
 * NPC entity state.
 * Updated from GameStateResponsePacket.
 */
public class NpcEntity {
    private final int id;
    private int npcType;
    private float x, y;
    private int hp, maxHp;
    private int shield, maxShield;
    private float speed;
    private long lastUpdateTime;
    private float lastX, lastY;

    public NpcEntity(int id) {
        this.id = id;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // === Getters ===

    public int getId() { return id; }
    public int getNpcType() { return npcType; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getShield() { return shield; }
    public int getMaxShield() { return maxShield; }
    public float getSpeed() { return speed; }

    // === Setters ===

    public void setNpcType(int npcType) { this.npcType = npcType; }

    public void setPosition(float x, float y) {
        this.lastX = this.x;
        this.lastY = this.y;
        this.x = x;
        this.y = y;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void setHp(int hp) { this.hp = hp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setShield(int shield) { this.shield = shield; }
    public void setMaxShield(int maxShield) { this.maxShield = maxShield; }
    public void setSpeed(float speed) { this.speed = speed; }

    // === Computed ===

    public boolean isDead() {
        return maxHp > 0 && hp <= 0;
    }

    public float hpPercent() {
        return maxHp > 0 ? (float) hp / maxHp : 1.0f;
    }

    public float distanceTo(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Predict NPC position using linear extrapolation.
     *
     * @param seconds time in seconds to predict ahead
     * @return float[2] with predicted {x, y}
     */
    public float[] predictPosition(float seconds) {
        if (speed <= 0 || lastUpdateTime == 0) {
            return new float[]{x, y};
        }

        // Calculate direction from last position
        float dx = x - lastX;
        float dy = y - lastY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 1) {
            return new float[]{x, y};
        }

        // Normalize direction and apply speed
        float dirX = dx / dist;
        float dirY = dy / dist;

        float predictedX = x + dirX * speed * seconds;
        float predictedY = y + dirY * speed * seconds;

        return new float[]{predictedX, predictedY};
    }

    @Override
    public String toString() {
        return String.format("NPC{id=%d, type=%d, pos=(%.0f,%.0f), hp=%d/%d, shield=%d/%d}",
                id, npcType, x, y, hp, maxHp, shield, maxShield);
    }
}
