package com.wubot.ship;

import com.wubot.protocol.packets.HangarInPacket;
import com.wubot.protocol.packets.equip.EquipResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages player's ships (hangars).
 * Tracks ship cargo status and enables ship switching for accumulation strategy.
 */
public class ShipManager {
    private static final Logger log = LoggerFactory.getLogger(ShipManager.class);

    private final List<ShipInfo> ships;
    private int currentShipIndex;

    public ShipManager() {
        this.ships = new ArrayList<>();
        this.currentShipIndex = -1;
    }

    /**
     * Update ships from EquipResponsePacket.
     *
     * @param packet equipment response containing hangar data
     */
    public void updateShips(EquipResponsePacket packet) {
        if (packet == null || packet.hangars == null) {
            return;
        }

        ships.clear();
        currentShipIndex = -1;

        for (int i = 0; i < packet.hangars.length; i++) {
            HangarInPacket hangar = packet.hangars[i];
            if (hangar == null || hangar.ship == null) continue;

            ShipInfo ship = new ShipInfo(
                    hangar.id,
                    hangar.ship.shipName,
                    hangar.ship.cargo
            );
            ship.setActivated(hangar.activated);
            ships.add(ship);

            if (hangar.activated) {
                currentShipIndex = ships.size() - 1;
            }
        }

        log.info("Updated {} ships, current ship index: {}", ships.size(), currentShipIndex);
        for (ShipInfo ship : ships) {
            log.debug("  {}", ship);
        }
    }

    /**
     * Update current ship's cargo from player state.
     *
     * @param cargoUsed current cargo used
     * @param cargoMax  maximum cargo
     */
    public void updateCurrentCargo(int cargoUsed, int cargoMax) {
        ShipInfo current = getCurrentShip();
        if (current != null) {
            current.setCargoUsed(cargoUsed);
            current.setCargoMax(cargoMax);
        }
    }

    /**
     * Get current active ship.
     */
    public ShipInfo getCurrentShip() {
        if (currentShipIndex >= 0 && currentShipIndex < ships.size()) {
            return ships.get(currentShipIndex);
        }
        return null;
    }

    /**
     * Check if current ship's cargo is full.
     */
    public boolean isCurrentCargoFull() {
        ShipInfo current = getCurrentShip();
        return current != null && current.isCargoFull();
    }

    /**
     * Check if there's another ship with available cargo space.
     */
    public boolean hasAvailableShip() {
        return getNextAvailableShip() != null;
    }

    /**
     * Get next ship with available cargo space.
     *
     * @return ship with cargo space, or null if all are full
     */
    public ShipInfo getNextAvailableShip() {
        for (int i = 0; i < ships.size(); i++) {
            if (i != currentShipIndex) {
                ShipInfo ship = ships.get(i);
                if (ship.hasCargoSpace()) {
                    return ship;
                }
            }
        }
        return null;
    }

    /**
     * Mark a ship as the current active ship.
     *
     * @param shipId ship ID to activate
     */
    public void setCurrentShip(int shipId) {
        for (int i = 0; i < ships.size(); i++) {
            ShipInfo ship = ships.get(i);
            if (ship.getId() == shipId) {
                // Deactivate previous
                if (currentShipIndex >= 0 && currentShipIndex < ships.size()) {
                    ships.get(currentShipIndex).setActivated(false);
                }
                // Activate new
                ship.setActivated(true);
                currentShipIndex = i;
                log.info("Switched to ship: {}", ship);
                return;
            }
        }
        log.warn("Ship with id {} not found", shipId);
    }

    /**
     * Get all ships.
     */
    public List<ShipInfo> getShips() {
        return ships;
    }

    /**
     * Get ship count.
     */
    public int getShipCount() {
        return ships.size();
    }

    /**
     * Get ships with available cargo space.
     */
    public List<ShipInfo> getShipsWithCargoSpace() {
        List<ShipInfo> available = new ArrayList<>();
        for (ShipInfo ship : ships) {
            if (ship.hasCargoSpace()) {
                available.add(ship);
            }
        }
        return available;
    }

    /**
     * Get total cargo status across all ships.
     */
    public String getCargoSummary() {
        int totalUsed = 0;
        int totalMax = 0;
        int fullCount = 0;

        for (ShipInfo ship : ships) {
            totalUsed += ship.getCargoUsed();
            totalMax += ship.getCargoMax();
            if (ship.isCargoFull()) {
                fullCount++;
            }
        }

        return String.format("%d/%d cargo (%d/%d ships full)",
                totalUsed, totalMax, fullCount, ships.size());
    }

    /**
     * Check if all ships have full cargo.
     */
    public boolean areAllShipsFull() {
        if (ships.isEmpty()) return false;

        for (ShipInfo ship : ships) {
            if (ship.hasCargoSpace()) {
                return false;
            }
        }
        return true;
    }
}
