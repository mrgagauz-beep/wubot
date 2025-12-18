package com.wubot.world;

/**
 * Current player state.
 * Updated from GameStateResponsePacket and UserInfoResponsePacket.
 */
public class PlayerState {
    private float x, y;
    private int hp, maxHp;
    private int shield, maxShield;
    private float speed;
    private int cargoUsed, cargoMax;
    private boolean inSafeZone;
    private int configId;

    // === Getters ===

    public float getX() { return x; }
    public float getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getShield() { return shield; }
    public int getMaxShield() { return maxShield; }
    public float getSpeed() { return speed; }
    public int getCargoUsed() { return cargoUsed; }
    public int getCargoMax() { return cargoMax; }
    public boolean isInSafeZone() { return inSafeZone; }
    public int getConfigId() { return configId; }

    // === Setters ===

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setHp(int hp) { this.hp = hp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setShield(int shield) { this.shield = shield; }
    public void setMaxShield(int maxShield) { this.maxShield = maxShield; }
    public void setSpeed(float speed) { this.speed = speed; }
    public void setCargoUsed(int cargoUsed) { this.cargoUsed = cargoUsed; }
    public void setCargoMax(int cargoMax) { this.cargoMax = cargoMax; }
    public void setInSafeZone(boolean inSafeZone) { this.inSafeZone = inSafeZone; }
    public void setConfigId(int configId) { this.configId = configId; }

    // === Computed ===

    public float hpPercent() {
        return maxHp > 0 ? (float) hp / maxHp : 0;
    }

    public float shieldPercent() {
        return maxShield > 0 ? (float) shield / maxShield : 0;
    }

    public boolean isCargoFull() {
        return cargoMax > 0 && cargoUsed >= cargoMax;
    }

    public boolean isHpCritical(float threshold) {
        return hpPercent() < threshold;
    }

    public float distanceTo(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("Player{pos=(%.0f,%.0f), hp=%d/%d, shield=%d/%d, cargo=%d/%d, safe=%s}",
                x, y, hp, maxHp, shield, maxShield, cargoUsed, cargoMax, inSafeZone);
    }
}
