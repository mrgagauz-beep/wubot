package com.wubot.world;

/**
 * Other player entity state.
 * Updated from GameStateResponsePacket.
 */
public class PlayerEntity {
    private final int id;
    private String name;
    private float x, y;
    private int hp, maxHp;
    private int shield, maxShield;
    private int clanId;
    private boolean isEnemy;
    private long lastUpdateTime;

    public PlayerEntity(int id) {
        this.id = id;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // === Getters ===

    public int getId() { return id; }
    public String getName() { return name; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getShield() { return shield; }
    public int getMaxShield() { return maxShield; }
    public int getClanId() { return clanId; }
    public boolean isEnemy() { return isEnemy; }
    public long getLastUpdateTime() { return lastUpdateTime; }

    // === Setters ===

    public void setName(String name) { this.name = name; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void setHp(int hp) { this.hp = hp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setShield(int shield) { this.shield = shield; }
    public void setMaxShield(int maxShield) { this.maxShield = maxShield; }
    public void setClanId(int clanId) { this.clanId = clanId; }
    public void setEnemy(boolean enemy) { this.isEnemy = enemy; }

    // === Computed ===

    public boolean isDead() {
        return maxHp > 0 && hp <= 0;
    }

    public float hpPercent() {
        return maxHp > 0 ? (float) hp / maxHp : 1.0f;
    }

    public float shieldPercent() {
        return maxShield > 0 ? (float) shield / maxShield : 0f;
    }

    public float distanceTo(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Check if this player is a threat (enemy with HP).
     */
    public boolean isThreat() {
        return isEnemy && !isDead();
    }

    @Override
    public String toString() {
        return String.format("Player{id=%d, name='%s', pos=(%.0f,%.0f), hp=%d/%d, enemy=%s}",
                id, name, x, y, hp, maxHp, isEnemy);
    }
}
