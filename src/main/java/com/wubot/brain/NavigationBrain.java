package com.wubot.brain;

import com.wubot.action.Action;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation logic - evacuation, portals, repair.
 */
public class NavigationBrain {
    private static final Logger log = LoggerFactory.getLogger(NavigationBrain.class);

    /** Distance to portal to trigger teleport */
    private static final float PORTAL_JUMP_DISTANCE = 150f;

    /** Factions by map ID ranges (approximate) */
    private static final int MMO_FACTION = 1;  // Maps 1-3
    private static final int EIC_FACTION = 2;  // Maps 4-6
    private static final int VRU_FACTION = 3;  // Maps 7-9

    // Navigation state
    private Integer targetMapId = null;
    private Integer targetPortalIndex = null;
    private boolean isEvacuating = false;

    // Track last map/portal for teleportation
    private int lastMapId = -1;
    private int lastPortalIndex = -1;

    // Portal data from last MapInfoPacket
    private PortalInfo[] currentPortals = null;
    private int currentMapId = -1;

    /**
     * Simple portal info holder.
     */
    public static class PortalInfo {
        public final int index;
        public final int type;
        public final int subtype;
        public final float x, y;

        public PortalInfo(int index, int type, int subtype, float x, float y) {
            this.index = index;
            this.type = type;
            this.subtype = subtype;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Update portal data when map changes.
     */
    public void setCurrentMapPortals(int mapId, PortalInfo[] portals) {
        this.currentMapId = mapId;
        this.currentPortals = portals;
    }

    /**
     * Get evacuation actions (flee to safety).
     */
    public List<Action> getEvacuationActions(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        // If already in safe zone, we're done
        if (world.isInSafeZone()) {
            log.info("Evacuation complete - reached safe zone");
            isEvacuating = false;
            return actions;
        }

        isEvacuating = true;

        // Find nearest portal
        PortalInfo portal = findNearestPortal(world);
        if (portal == null) {
            // No portal found - move towards safe zone center
            log.warn("No portal found, moving to safe zone center");
            actions.add(new Action.Move(world.getSafeZoneX(), world.getSafeZoneY()));
            return actions;
        }

        // Calculate distance to portal
        float dist = world.distanceTo(portal.x, portal.y);

        if (dist <= PORTAL_JUMP_DISTANCE) {
            // Close enough - teleport!
            log.info("Near portal, teleporting! (index={})", portal.index);
            lastMapId = currentMapId;
            lastPortalIndex = portal.index;
            actions.add(new Action.UseTeleport(portal.index));
        } else {
            // Move towards portal (and beyond - don't stop at it!)
            // Calculate point beyond portal
            float playerX = world.getPlayerX();
            float playerY = world.getPlayerY();
            float dx = portal.x - playerX;
            float dy = portal.y - playerY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                dx /= len;
                dy /= len;
            }
            // Target point beyond portal
            float targetX = portal.x + dx * BotConfig.FLEE_BEYOND_PORTAL;
            float targetY = portal.y + dy * BotConfig.FLEE_BEYOND_PORTAL;

            log.debug("Moving towards portal at ({}, {})", portal.x, portal.y);
            actions.add(new Action.Move(targetX, targetY));
        }

        return actions;
    }

    /**
     * Handle map change during evacuation.
     * Returns actions to take (e.g., instant jump back if enemy map).
     */
    public List<Action> onMapChanged(int newMapId, int playerFaction, WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        log.info("Map changed: {} -> {}", lastMapId, newMapId);

        if (!isEvacuating) {
            return actions;
        }

        // Check if new map is allied
        if (isAlliedMap(newMapId, playerFaction)) {
            log.info("Arrived on allied map {} - evacuation success!", newMapId);
            isEvacuating = false;
            // We're on portal in safe zone - start repair
        } else {
            // Enemy or PvP map - instant jump back!
            // We're already ON the portal after teleport
            log.warn("Arrived on enemy/PvP map {} - jumping back!", newMapId);

            // Find portal we're standing on (nearest to player)
            PortalInfo standingPortal = findNearestPortal(world);
            if (standingPortal != null) {
                actions.add(new Action.UseTeleport(standingPortal.index));
            }
        }

        return actions;
    }

    /**
     * Get repair actions (just wait in safe zone).
     */
    public List<Action> getRepairActions(WorldSnapshot world) {
        // Repair is automatic in safe zone - just return empty list
        log.trace("Repairing in safe zone - HP: {}/{}", world.getPlayerHp(), world.getPlayerMaxHp());
        return new ArrayList<>();
    }

    /**
     * Find nearest portal to player.
     * Uses hardcoded portal positions if no MapInfoPacket was received.
     */
    public PortalInfo findNearestPortal(WorldSnapshot world) {
        PortalInfo[] portals = currentPortals;
        
        // If no portals from MapInfoPacket, use hardcoded positions for known maps
        if (portals == null || portals.length == 0) {
            portals = getHardcodedPortals(currentMapId);
            if (portals != null) {
                log.info("[NAV] Using hardcoded portals for mapId={} ({} portals)", currentMapId, portals.length);
            }
        }
        
        if (portals == null || portals.length == 0) {
            log.warn("[NAV] No portals available for mapId={}", currentMapId);
            return null;
        }

        PortalInfo nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (PortalInfo portal : portals) {
            float dist = world.distanceTo(portal.x, portal.y);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = portal;
            }
        }

        if (nearest != null) {
            log.debug("[NAV] Nearest portal: index={} at ({}, {}), dist={}", 
                nearest.index, (int)nearest.x, (int)nearest.y, (int)nearestDist);
        }

        return nearest;
    }
    
    /**
     * Get hardcoded portal positions for known maps.
     * Based on packet analysis from user's packet_report.txt.
     * 
     * Map E-1 (mapId=2): 
     *   - Portal 0: x=15000, y=1000
     *   - Portal 1: x=1000, y=1000
     */
    private PortalInfo[] getHardcodedPortals(int mapId) {
        // If mapId is unknown (0 or -1), default to E-1 (mapId=2) since that's where user typically is
        int effectiveMapId = (mapId <= 0) ? 2 : mapId;
        
        switch (effectiveMapId) {
            case 2: // E-1
                return new PortalInfo[] {
                    new PortalInfo(0, 0, 0, 15000, 1000),
                    new PortalInfo(1, 0, 0, 1000, 1000)
                };
            case 1: // E-0 (starter map, approximate)
                return new PortalInfo[] {
                    new PortalInfo(0, 0, 0, 15000, 5000),
                    new PortalInfo(1, 0, 0, 1000, 5000)
                };
            default:
                // For unknown maps, return E-1 portals as fallback
                log.warn("[NAV] Unknown mapId={}, using E-1 portals as fallback", mapId);
                return new PortalInfo[] {
                    new PortalInfo(0, 0, 0, 15000, 1000),
                    new PortalInfo(1, 0, 0, 1000, 1000)
                };
        }
    }

    /**
     * Check if map is allied for given faction.
     * Allied maps: X-1 to X-3 of own faction, and own X-7.
     */
    public boolean isAlliedMap(int mapId, int playerFaction) {
        // PvP maps - never safe
        if (isPvPMap(mapId)) {
            return false;
        }

        // Determine map's faction from ID (approximate)
        int mapFaction = getMapFaction(mapId);

        // Allied if same faction
        return mapFaction == playerFaction;
    }

    /**
     * Check if map is a PvP zone (Junction, T-1, G-1).
     */
    public boolean isPvPMap(int mapId) {
        // Junction maps typically have specific IDs
        // This is approximate - real implementation would use map name
        // J-VO, J-VS, J-SO, T-1, G-1 are PvP
        String mapName = getMapNameById(mapId);
        if (mapName == null) return false;

        return mapName.startsWith("J-") ||
               mapName.equals("T-1") ||
               mapName.equals("G-1");
    }

    /**
     * Get faction owning a map (1=MMO, 2=EIC, 3=VRU).
     * This is approximate based on typical map ID patterns.
     */
    private int getMapFaction(int mapId) {
        // Typical pattern: each faction has 7 maps
        // MMO: 1-7, EIC: 8-14, VRU: 15-21 (example)
        // Real implementation would use map name prefix (E-, U-, R-)
        if (mapId <= 7) return MMO_FACTION;
        if (mapId <= 14) return EIC_FACTION;
        return VRU_FACTION;
    }

    /**
     * Get map name by ID (placeholder - needs real data).
     */
    private String getMapNameById(int mapId) {
        // TODO: Implement with real map registry
        return null;
    }

    /**
     * Get actions to move to nearest portal (for ship switching).
     */
    public List<Action> getMoveToNearestPortalActions(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        PortalInfo portal = findNearestPortal(world);
        if (portal == null) {
            log.warn("No portal found for ship switching");
            return actions;
        }

        float dist = world.distanceTo(portal.x, portal.y);

        // If we're close to portal, server should mark us as in safe zone
        // Just move towards the portal
        if (dist > PORTAL_JUMP_DISTANCE) {
            log.debug("Moving to portal for ship switch ({}, {})", portal.x, portal.y);
            actions.add(new Action.Move(portal.x, portal.y));
        }

        return actions;
    }

    // === Getters/Setters ===

    public boolean isEvacuating() {
        return isEvacuating;
    }

    public void startEvacuation() {
        isEvacuating = true;
    }

    public void stopEvacuation() {
        isEvacuating = false;
    }

    public int getLastMapId() {
        return lastMapId;
    }

    public int getLastPortalIndex() {
        return lastPortalIndex;
    }
}
