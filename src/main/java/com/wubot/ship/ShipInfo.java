package com.wubot.ship;

/**
 * Information about a player's ship in the hangar.
 */
public class ShipInfo {
    private final int id;
    private final String name;
    private int cargoUsed;
    private int cargoMax;
    private int configId;
    private boolean activated;

    public ShipInfo(int id, String name, int cargoMax) {
        this.id = id;
        this.name = name;
        this.cargoUsed = 0;
        this.cargoMax = cargoMax;
        this.configId = 1;
        this.activated = false;
    }

    /**
     * Check if cargo is full.
     */
    public boolean isCargoFull() {
        return cargoMax > 0 && cargoUsed >= cargoMax;
    }

    /**
     * Get cargo fill percentage (0-100).
     */
    public float getCargoPercent() {
        if (cargoMax <= 0) return 0f;
        return (cargoUsed * 100f) / cargoMax;
    }

    /**
     * Check if ship has available cargo space.
     */
    public boolean hasCargoSpace() {
        return cargoMax > 0 && cargoUsed < cargoMax;
    }

    // === Getters and Setters ===

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCargoUsed() {
        return cargoUsed;
    }

    public void setCargoUsed(int cargoUsed) {
        this.cargoUsed = cargoUsed;
    }

    public int getCargoMax() {
        return cargoMax;
    }

    public void setCargoMax(int cargoMax) {
        this.cargoMax = cargoMax;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public String toString() {
        return String.format("ShipInfo{id=%d, name='%s', cargo=%d/%d (%.1f%%), active=%s}",
                id, name, cargoUsed, cargoMax, getCargoPercent(), activated);
    }
}
