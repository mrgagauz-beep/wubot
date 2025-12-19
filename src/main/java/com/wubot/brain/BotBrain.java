package com.wubot.brain;

import com.wubot.action.Action;
import com.wubot.config.BotConfig;
import com.wubot.ship.ShipInfo;
import com.wubot.ship.ShipManager;
import com.wubot.world.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main bot AI - FSM-based decision making.
 */
public class BotBrain {
    private static final Logger log = LoggerFactory.getLogger(BotBrain.class);

    /**
     * Phases of the REPAIRING state.
     * REPAIR_ESCAPE: On Config 2 - repair HP + Shield to 100%
     * SWITCH_TO_FARM: Switch to Config 1
     * REPAIR_FARM: On Config 1 - repair Shield to 100%
     */
    public enum RepairPhase {
        REPAIR_ESCAPE,
        SWITCH_TO_FARM,
        REPAIR_FARM
    }

    private BotState state = BotState.IDLE;
    private final SafetyBrain safetyBrain;
    private final CombatBrain combatBrain;
    private final CollectBrain collectBrain;
    private final NavigationBrain navigationBrain;
    private final ShipManager shipManager;

    // Player faction (1=MMO, 2=EIC, 3=VRU) - should be set from config
    private int playerFaction = 1;

    // Ship switching state
    private ShipInfo targetShip = null;

    // Repair phase for multi-phase REPAIRING state
    private RepairPhase repairPhase = RepairPhase.REPAIR_ESCAPE;

    public BotBrain() {
        this.safetyBrain = new SafetyBrain();
        this.combatBrain = new CombatBrain();
        this.collectBrain = new CollectBrain();
        this.navigationBrain = new NavigationBrain();
        this.shipManager = new ShipManager();
    }

    /**
     * Make decisions based on current world state.
     *
     * @param world current world snapshot
     * @return list of actions to execute
     */
    public List<Action> decide(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();

        // Safety check first - always takes priority
        if (state != BotState.FLEEING && state != BotState.REPAIRING) {
            if (safetyBrain.shouldFlee(world)) {
                transitionTo(BotState.FLEEING);
            }
        }

        // State machine
        switch (state) {
            case IDLE -> decideIdle(world, actions);
            case FARMING -> decideFarming(world, actions);
            case COLLECTING -> decideCollecting(world, actions);
            case FLEEING -> decideFleeing(world, actions);
            case REPAIRING -> decideRepairing(world, actions);
            case NAVIGATING -> decideNavigating(world, actions);
            case SWITCHING_SHIP -> decideSwitchingShip(world, actions);
            case EXPLORING -> decideExploring(world, actions);
        }

        return actions;
    }

    /**
     * Transition to a new state with logging.
     */
    private void transitionTo(BotState newState) {
        if (state != newState) {
            log.info("State transition: {} -> {}", state, newState);
            state = newState;

            // Reset repair phase when entering REPAIRING state
            if (newState == BotState.REPAIRING) {
                repairPhase = RepairPhase.REPAIR_ESCAPE;
                log.info("REPAIRING: Starting phase {}", repairPhase);
            }
        }
    }

    // === State handlers (stubs for now) ===

    private void decideIdle(WorldSnapshot world, List<Action> actions) {
        log.debug("IDLE state - transitioning to FARMING");
        transitionTo(BotState.FARMING);
    }

    // Patrol state for when no targets/boxes are found
    private float patrolTargetX = -1;
    private float patrolTargetY = -1;
    private long lastPatrolTime = 0;
    private static final long PATROL_INTERVAL_MS = 8000; // Change patrol point every 8 seconds
    
    private void decideFarming(WorldSnapshot world, List<Action> actions) {
        log.trace("FARMING state - combat + collection");

        // Update ship cargo from player state
        shipManager.updateCurrentCargo(world.getCargoUsed(), world.getCargoMax());

        // Check if cargo is full
        boolean cargoFull = shipManager.isCurrentCargoFull();
        if (cargoFull) {
            if (shipManager.hasAvailableShip()) {
                // Switch to another ship with cargo space
                targetShip = shipManager.getNextAvailableShip();
                log.info("Cargo full! Switching to ship: {}", targetShip);
                transitionTo(BotState.SWITCHING_SHIP);
                return;
            } else {
                // All ships full - continue farming, but only collect BONUS boxes
                log.debug("All ships full - farming with bonus box collection only");
            }
        }

        // Combat actions (target selection, kiting, attacking)
        List<Action> combatActions = combatBrain.getActions(world);
        actions.addAll(combatActions);

        // Collection actions (parallel with combat!)
        // ALWAYS call CollectBrain - it handles cargo full internally (only collects BONUS boxes when full)
        List<Action> collectActions = collectBrain.getActions(world);
        actions.addAll(collectActions);

        // If no combat target and no collection happening, patrol the map
        if (combatActions.isEmpty() && collectActions.isEmpty()) {
            log.debug("No targets or boxes found - patrolling");
            addPatrolAction(world, actions);
        }
    }
    
    /**
     * Add patrol action to move to a random point on the map.
     * Changes patrol target every PATROL_INTERVAL_MS.
     */
    private void addPatrolAction(WorldSnapshot world, List<Action> actions) {
        long now = System.currentTimeMillis();
        
        // Check if we need a new patrol target
        if (patrolTargetX < 0 || now - lastPatrolTime > PATROL_INTERVAL_MS) {
            // Generate new random patrol point within map bounds
            float mapWidth = world.getMapWidth();
            float mapHeight = world.getMapHeight();
            
            // If map dimensions not available yet, use default patrol around current position
            if (mapWidth <= 0 || mapHeight <= 0) {
                float playerX = world.getPlayerX();
                float playerY = world.getPlayerY();
                // Patrol in a 2000px radius around current position
                float radius = 2000f;
                float angle = (float) (Math.random() * 2 * Math.PI);
                patrolTargetX = playerX + (float) Math.cos(angle) * radius * (0.5f + (float) Math.random() * 0.5f);
                patrolTargetY = playerY + (float) Math.sin(angle) * radius * (0.5f + (float) Math.random() * 0.5f);
                // Keep coordinates positive
                patrolTargetX = Math.max(100, patrolTargetX);
                patrolTargetY = Math.max(100, patrolTargetY);
                lastPatrolTime = now;
                log.info("[PATROL] New patrol target (no map info): ({}, {})", patrolTargetX, patrolTargetY);
                return;
            }
            
            // Stay away from edges (10% margin)
            float margin = 0.1f;
            patrolTargetX = mapWidth * (margin + (float) Math.random() * (1 - 2 * margin));
            patrolTargetY = mapHeight * (margin + (float) Math.random() * (1 - 2 * margin));
            lastPatrolTime = now;
            
            log.info("[PATROL] New patrol target: ({}, {})", patrolTargetX, patrolTargetY);
        }
        
        // Check if we're close to patrol target
        float distToTarget = world.distanceTo(patrolTargetX, patrolTargetY);
        if (distToTarget < 200) {
            // Reached target, will get new one next tick
            patrolTargetX = -1;
            log.debug("[PATROL] Reached patrol target, will select new one");
            return;
        }
        
        // Move towards patrol target
        actions.add(new Action.Move(patrolTargetX, patrolTargetY));
    }

    private void decideCollecting(WorldSnapshot world, List<Action> actions) {
        log.trace("COLLECTING state - looking for boxes");

        // Only collection, no combat
        List<Action> collectActions = collectBrain.getActions(world);
        actions.addAll(collectActions);

        // If no boxes, go back to farming
        if (collectActions.isEmpty()) {
            log.debug("No boxes to collect, returning to FARMING");
            transitionTo(BotState.FARMING);
        }
    }

    private void decideFleeing(WorldSnapshot world, List<Action> actions) {
        // Switch to escape config (speed + defense) if not already
        int currentConfig = world.getConfigId();
        log.info("FLEEING: currentConfig={}, ESCAPE_CONFIG={}", currentConfig, BotConfig.ESCAPE_CONFIG);
        if (currentConfig != BotConfig.ESCAPE_CONFIG) {
            log.info("FLEEING: Switching to escape config");
            log.info("FLEEING: Switching to escape config {} (was {})",
                     BotConfig.ESCAPE_CONFIG, currentConfig);
            actions.add(new Action.SwitchConfig(BotConfig.ESCAPE_CONFIG));
        }

        // Check if we reached safety
        if (safetyBrain.isInSafeZone(world)) {
            log.info("Reached safe zone!");
            navigationBrain.stopEvacuation();
            transitionTo(BotState.REPAIRING);
            return;
        }

        // Use NavigationBrain for evacuation
        log.debug("FLEEING state - evacuating");
        List<Action> evacuationActions = navigationBrain.getEvacuationActions(world);
        actions.addAll(evacuationActions);
    }

    private void decideRepairing(WorldSnapshot world, List<Action> actions) {
        // Check if we left safe zone
        if (!safetyBrain.isInSafeZone(world)) {
            log.warn("Left safe zone during repair!");
            transitionTo(BotState.FLEEING);
            return;
        }

        int currentConfig = world.getConfigId();

        switch (repairPhase) {
            case REPAIR_ESCAPE -> {
                // Проверяем что мы на ESCAPE конфигурации
                if (currentConfig != BotConfig.ESCAPE_CONFIG) {
                    log.info("REPAIR_ESCAPE: switching to escape config {} (current={})",
                             BotConfig.ESCAPE_CONFIG, currentConfig);
                    actions.add(new Action.SwitchConfig(BotConfig.ESCAPE_CONFIG));
                    return;  // Ждём следующий тик когда configId обновится
                }

                // Phase 1: On Config 2 (escape) - repair HP + Shield to 100%
                log.debug("REPAIRING phase REPAIR_ESCAPE: HP={}/{}, Shield={}/{}, Config={}",
                         world.getPlayerHp(), world.getPlayerMaxHp(),
                         world.getPlayerShield(), world.getPlayerMaxShield(),
                         currentConfig);

                if (safetyBrain.isFullyRepaired(world)) {
                    log.info("REPAIRING: Phase REPAIR_ESCAPE complete (HP+Shield 100%), moving to SWITCH_TO_FARM");
                    repairPhase = RepairPhase.SWITCH_TO_FARM;
                    // Fall through to next phase immediately
                    decideRepairing(world, actions);
                    return;
                }

                // Wait for auto-repair
                List<Action> repairActions = navigationBrain.getRepairActions(world);
                actions.addAll(repairActions);
            }

            case SWITCH_TO_FARM -> {
                // Phase 2: Switch to Config 1 (farm) - wait for server confirmation
                if (currentConfig == BotConfig.FARM_CONFIG) {
                    // Server confirmed switch to FARM config
                    log.info("REPAIRING: Config switched to FARM confirmed, starting REPAIR_FARM");
                    repairPhase = RepairPhase.REPAIR_FARM;
                } else {
                    // Not yet switched - send command (or wait for confirmation)
                    log.debug("REPAIRING phase SWITCH_TO_FARM: waiting for Config {} (current={})",
                              BotConfig.FARM_CONFIG, currentConfig);
                    actions.add(new Action.SwitchConfig(BotConfig.FARM_CONFIG));
                }
            }

            case REPAIR_FARM -> {
                // Проверяем что мы действительно на FARM конфигурации
                if (currentConfig != BotConfig.FARM_CONFIG) {
                    log.warn("REPAIR_FARM: unexpected config {} (expected {}), returning to SWITCH_TO_FARM",
                             currentConfig, BotConfig.FARM_CONFIG);
                    repairPhase = RepairPhase.SWITCH_TO_FARM;
                    return;
                }

                // Phase 3: On Config 1 (farm) - repair Shield to 100%
                log.debug("REPAIRING phase REPAIR_FARM: Shield={}/{}, Config={}",
                         world.getPlayerShield(), world.getPlayerMaxShield(), currentConfig);

                // Check if Shield is fully repaired on farm config
                if (world.getPlayerShield() >= world.getPlayerMaxShield()) {
                    log.info("REPAIRING: Phase REPAIR_FARM complete (Shield 100%), transitioning to FARMING");
                    transitionTo(BotState.FARMING);
                    return;
                }

                // Wait for shield to regenerate
                List<Action> repairActions = navigationBrain.getRepairActions(world);
                actions.addAll(repairActions);
            }
        }
    }

    private void decideNavigating(WorldSnapshot world, List<Action> actions) {
        // TODO: Implement inter-map navigation
        log.trace("NAVIGATING state");
    }

    private void decideSwitchingShip(WorldSnapshot world, List<Action> actions) {
        log.trace("SWITCHING_SHIP state");

        // Check if we have a target ship
        if (targetShip == null) {
            log.warn("No target ship set, returning to FARMING");
            transitionTo(BotState.FARMING);
            return;
        }

        // Ship switching can only be done in safe zone
        if (!safetyBrain.isInSafeZone(world)) {
            // Need to get to safe zone first - fly to nearest portal
            log.debug("Not in safe zone - flying to portal for ship switch");
            List<Action> navActions = navigationBrain.getMoveToNearestPortalActions(world);
            actions.addAll(navActions);

            // Check if we reached a portal and are now in safe zone
            // The safe zone status comes from server
            return;
        }

        // We're in safe zone - can switch ship!
        log.info("In safe zone - switching to ship: {}", targetShip);
        actions.add(new Action.SwitchShip(targetShip.getId()));

        // Update ship manager
        shipManager.setCurrentShip(targetShip.getId());
        targetShip = null;

        // Go back to farming
        transitionTo(BotState.FARMING);
    }

    // Exploration state tracking
    private int explorationJumpCount = 0;
    private int explorationTargetJumps = 2;  // Jump there and back
    private boolean waitingForMapChange = false;
    
    /**
     * Start exploration mode - teleport to another map and back.
     */
    public void startExploration() {
        explorationJumpCount = 0;
        waitingForMapChange = false;
        transitionTo(BotState.EXPLORING);
        log.info("[EXPLORE] Starting exploration - will teleport {} times", explorationTargetJumps);
    }
    
    private void decideExploring(WorldSnapshot world, List<Action> actions) {
        log.debug("[EXPLORE] state - jumpCount={}/{}, waitingForMapChange={}", 
                  explorationJumpCount, explorationTargetJumps, waitingForMapChange);
        
        // Check if exploration is complete
        if (explorationJumpCount >= explorationTargetJumps) {
            log.info("[EXPLORE] Exploration complete! {} jumps done. Returning to FARMING.", explorationJumpCount);
            transitionTo(BotState.FARMING);
            return;
        }
        
        // If waiting for map change, don't do anything
        if (waitingForMapChange) {
            log.debug("[EXPLORE] Waiting for map change confirmation...");
            return;
        }
        
        // Find nearest portal
        NavigationBrain.PortalInfo portal = navigationBrain.findNearestPortal(world);
        if (portal == null) {
            log.warn("[EXPLORE] No portal found on this map!");
            transitionTo(BotState.FARMING);
            return;
        }
        
        // Calculate distance to portal
        float dist = world.distanceTo(portal.x, portal.y);
        log.info("[EXPLORE] Portal at ({}, {}), distance={}", portal.x, portal.y, (int) dist);
        
        if (dist <= 150f) {
            // Close enough - teleport!
            log.info("[EXPLORE] Near portal ({}px), teleporting! (index={})", (int) dist, portal.index);
            actions.add(new Action.UseTeleport(portal.index));
            waitingForMapChange = true;
        } else {
            // Move towards portal
            log.info("[EXPLORE] Moving to portal at ({}, {})", portal.x, portal.y);
            actions.add(new Action.Move(portal.x, portal.y));
        }
    }
    
    /**
     * Called when map changes during exploration.
     */
    public void onExplorationMapChanged(int newMapId) {
        if (state == BotState.EXPLORING && waitingForMapChange) {
            explorationJumpCount++;
            waitingForMapChange = false;
            log.info("[EXPLORE] Map changed to {}! Jump {}/{} complete.", 
                     newMapId, explorationJumpCount, explorationTargetJumps);
        }
    }

    // === Getters ===

    public BotState getState() {
        return state;
    }

    public SafetyBrain getSafetyBrain() {
        return safetyBrain;
    }

    public CombatBrain getCombatBrain() {
        return combatBrain;
    }

    public CollectBrain getCollectBrain() {
        return collectBrain;
    }

    public NavigationBrain getNavigationBrain() {
        return navigationBrain;
    }

    public ShipManager getShipManager() {
        return shipManager;
    }

    /**
     * Handle map change event.
     * Called by GameLoop when MapInfoPacket is received.
     */
    public List<Action> onMapChanged(int newMapId, WorldSnapshot world) {
        return navigationBrain.onMapChanged(newMapId, playerFaction, world);
    }

    /**
     * Update portal data from MapInfoPacket.
     */
    public void setCurrentMapPortals(int mapId, NavigationBrain.PortalInfo[] portals) {
        navigationBrain.setCurrentMapPortals(mapId, portals);
    }

    /**
     * Set player faction.
     */
    public void setPlayerFaction(int faction) {
        this.playerFaction = faction;
    }
}
