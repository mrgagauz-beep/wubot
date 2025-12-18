package com.wubot.protocol;

import com.wubot.discovery.DiscoveryCollector;
import com.wubot.protocol.packets.*;
import com.wubot.protocol.packets.equip.EquipResponsePacket;
import com.wubot.ship.ShipManager;
import com.wubot.world.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incoming packets and updates World state.
 */
public class PacketProcessor {
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);

    private final DiscoveryCollector discovery;
    private ShipManager shipManager;
    private int playerId = -1;
    private int currentMapId = -1;

    public PacketProcessor(DiscoveryCollector discovery) {
        this.discovery = discovery;
    }

    /**
     * Set ship manager for hangar updates.
     */
    public void setShipManager(ShipManager shipManager) {
        this.shipManager = shipManager;
    }

    /**
     * Process incoming packet and update world state.
     */
    public void process(Object packet, World world) {
        if (packet == null) return;

        if (packet instanceof GameStateResponsePacket p) {
            processGameState(p, world);
        } else if (packet instanceof MapInfoPacket p) {
            processMapInfo(p, world);
        } else if (packet instanceof CollectableInPacket p) {
            processCollectable(p, world);
        } else if (packet instanceof UserInfoResponsePacket p) {
            processUserInfo(p, world);
        } else if (packet instanceof EquipResponsePacket p) {
            processEquipResponse(p);
        } else if (packet instanceof MessageResponsePacket p) {
            processMessage(p);
        } else {
            log.trace("Unhandled packet: {}", packet.getClass().getSimpleName());
        }
    }

    /**
     * Process GameStateResponsePacket - main game state update.
     */
    private void processGameState(GameStateResponsePacket packet, World world) {
        // DEBUG: Log incoming packet data
        log.info("[DEBUG] GameState: ships={}, collectables={}",
            packet.ships != null ? packet.ships.length : 0,
            packet.collectables != null ? packet.collectables.length : 0);

        // Store player ID for distinguishing own ship
        if (packet.playerId > 0) {
            this.playerId = packet.playerId;
        }

        // Update safe zone status
        log.debug("Server safeZone flag: {}", packet.safeZone);
        world.getPlayer().setInSafeZone(packet.safeZone);

        // Update config ID
        if (packet.confi > 0) {
            int oldConfig = world.getPlayer().getConfigId();
            world.getPlayer().setConfigId(packet.confi);
            if (oldConfig != packet.confi) {
                log.info("CONFIG_SWITCHED: {} -> {}", oldConfig, packet.confi);
            }
        }

        // Process ships
        if (packet.ships != null) {
            for (GameStateResponsePacket.ShipInResponse ship : packet.ships) {
                processShip(ship, world);
            }
        }

        // DEBUG: Log world state after processing ships
        log.info("[DEBUG] World state after ships: npcs={}, players={}",
            world.getAllNpcs().size(), world.getAllPlayers().size());

        // Process collectables (boxes)
        if (packet.collectables != null) {
            // If flushCollectables is set, clear existing boxes first
            if (packet.flushCollectables) {
                world.clearBoxes();
            }
            for (CollectableInPacket c : packet.collectables) {
                processCollectable(c, world);
            }
        }
    }

    /**
     * Process a ship from GameStateResponsePacket.
     */
    private void processShip(GameStateResponsePacket.ShipInResponse ship, World world) {
        // Log damages and restores arrays - these might contain HP information!
        if (ship.damages != null && ship.damages.length > 0) {
            log.info("[SHIP {}] DAMAGES array: {}", ship.id, java.util.Arrays.toString(ship.damages));
        }
        if (ship.restores != null && ship.restores.length > 0) {
            log.info("[SHIP {}] RESTORES array: {}", ship.id, java.util.Arrays.toString(ship.restores));
        }
        
        if (ship.changes == null || ship.changes.length == 0) {
            return;
        }

        // Check if this is an NPC (has NPC_TYPE parameter)
        int npcType = extractNpcType(ship.changes);
        log.info("[DEBUG] processShip: id={}, npcType={}, changes={}, likelyNPC={}",
            ship.id, npcType, ship.changes != null ? ship.changes.length : 0,
            ship.id > 1000000 ? "YES" : "NO");

        if (npcType > 0) {
            // This is an NPC
            processNpc(ship, npcType, world);
        } else if (ship.id != playerId) {
            // This is another player
            processOtherPlayer(ship, world);
        } else {
            // This is our own ship
            processOwnShip(ship, world);
        }

        // Handle destroyed entities
        if (ship.destroyed) {
            if (npcType > 0) {
                // Record NPC kill for discovery statistics
                discovery.onNpcKilled(npcType);
                world.removeNpc(ship.id);
                log.debug("NPC {} (type={}) destroyed", ship.id, npcType);
            } else if (ship.id != playerId) {
                world.removePlayer(ship.id);
                log.debug("Player {} destroyed", ship.id);
            }
        }
    }

    /**
     * Extract NPC type from changes array.
     */
    private int extractNpcType(ChangedParameter[] changes) {
        if (changes == null) return 0;

        String entityName = null;

        // DEBUG: Log ALL change IDs to see what we're getting
        StringBuilder changeIds = new StringBuilder();
        for (ChangedParameter change : changes) {
            changeIds.append(change.id).append(" ");
        }
        log.info("[DEBUG] extractNpcType: all change.id = [{}]", changeIds.toString().trim());

        for (ChangedParameter change : changes) {
            // Check ENTITY_TYPE: 3 = NPC, 100 = Player
            if (change.id == ParamId.ENTITY_TYPE && change.data != null) {
                int entityType = ((Number) change.data).intValue();
                log.info("[DEBUG] extractNpcType: Found ENTITY_TYPE (id=42), value={}", entityType);
                // entityType == 3 means NPC
                if (entityType == 3) {
                    log.info("[DEBUG] extractNpcType: Confirmed NPC (ENTITY_TYPE=3)");
                    // Try to get real type from name
                    if (entityName != null) {
                        int realType = parseNpcTypeFromName(entityName);
                        return realType > 0 ? realType : 3;
                    }
                    return 3;  // Return 3 as default NPC type
                }
            }
            // Extract entity name for later parsing
            if (change.id == ParamId.NAME && change.data != null) {
                entityName = change.data.toString();
                log.info("[DEBUG] extractNpcType: Found NAME (id=12), value={}", entityName);
            }
        }
        return 0;
    }

    /**
     * Parse NPC type from entity name.
     * Examples: -=(Hydro)=- -> type Hydro
     *           -=(Hyper|Hydro)=- -> type Hyper|Hydro
     */
    private int parseNpcTypeFromName(String name) {
        if (name == null) return 0;

        // Extract type from name format: -=(Type)=-
        if (name.contains("Hydro")) return 1;
        if (name.contains("Hyper")) return 2;
        // Add more types as needed

        return 1; // default NPC type
    }

    /**
     * Process NPC entity update.
     */
    private void processNpc(GameStateResponsePacket.ShipInResponse ship, int npcType, World world) {
        NpcEntity npc = world.getOrCreateNpc(ship.id);
        npc.setNpcType(npcType);

        // Track maxHp before applying changes to detect when it becomes > 0
        int prevMaxHp = npc.getMaxHp();

        for (ChangedParameter change : ship.changes) {
            applyNpcChange(npc, change);
        }

        // Record NPC stats for discovery when maxHp becomes available (after lock)
        // This fixes the bug where stats were only recorded when isNew && maxHp > 0,
        // but maxHp is always 0 when NPC is first seen (before lock)
        if (!npc.isStatsRecorded() && currentMapId > 0 && npc.getMaxHp() > 0) {
            discovery.onNpcSpawn(currentMapId, npcType, npc.getHp(), npc.getMaxHp(), npc.getSpeed());
            npc.setStatsRecorded(true);
            log.info("[DISCOVERY] Recorded NPC type={} hp={}/{} speed={} on map={}", 
                npcType, npc.getHp(), npc.getMaxHp(), npc.getSpeed(), currentMapId);
        }

        log.trace("Updated NPC: {}", npc);
    }

    /**
     * Apply a parameter change to NPC.
     * 
     * IMPORTANT: NPC HP uses ParamId 25/26 (same as players), NOT 24/31!
     * ParamId 24/31 are UI scale values (e.g., 110/200) - NOT real HP.
     * Discovered from packet analysis: Hydro NPC has ParamId 25=800, ParamId 26=800.
     */
    private void applyNpcChange(NpcEntity npc, ChangedParameter change) {
        if (change.data == null) return;

        int newValue = 0;
        if (change.data instanceof Number) {
            newValue = ((Number) change.data).intValue();
        }

        try {
            if (change.id == ParamId.POSITION) {
                float[] pos = parsePosition(change.data);
                if (pos != null) {
                    npc.setPosition(pos[0], pos[1]);
                }
            } else if (change.id == ParamId.HP) {
                // ParamId 25 - REAL HP (same as players)
                int oldVal = npc.getHp();
                npc.setHp(newValue);
                if (oldVal != newValue) {
                    log.info("[HP CHANGE] NPC {} - HP: {} -> {} (delta={})", 
                        npc.getId(), oldVal, newValue, newValue - oldVal);
                }
            } else if (change.id == ParamId.MAX_HP) {
                // ParamId 26 - REAL MAX_HP (same as players)
                int oldVal = npc.getMaxHp();
                npc.setMaxHp(newValue);
                if (oldVal != newValue) {
                    log.info("[HP CHANGE] NPC {} - MAX_HP: {} -> {} (delta={})", 
                        npc.getId(), oldVal, newValue, newValue - oldVal);
                }
            } else if (change.id == ParamId.UI_HP_SCALE) {
                // ParamId 24 - UI scale value (NOT real HP, ignore for HP tracking)
                log.trace("[UI SCALE] NPC {} - ParamId 24 (UI scale): {}", npc.getId(), newValue);
            } else if (change.id == ParamId.UI_MAX_HP_SCALE) {
                // ParamId 31 - UI scale value (NOT real HP, ignore for HP tracking)
                log.trace("[UI SCALE] NPC {} - ParamId 31 (UI scale): {}", npc.getId(), newValue);
            } else if (change.id == ParamId.SHIELD) {
                // ParamId 27
                int oldVal = npc.getShield();
                npc.setShield(newValue);
                if (oldVal != newValue) {
                    log.info("[SHIELD CHANGE] NPC {} - Shield: {} -> {} (delta={})", 
                        npc.getId(), oldVal, newValue, newValue - oldVal);
                }
            } else if (change.id == ParamId.MAX_SHIELD) {
                // ParamId 28
                int oldVal = npc.getMaxShield();
                npc.setMaxShield(newValue);
                if (oldVal != newValue) {
                    log.info("[SHIELD CHANGE] NPC {} - Max Shield: {} -> {}", 
                        npc.getId(), oldVal, newValue);
                }
            } else if (change.id == ParamId.SPEED) {
                npc.setSpeed(((Number) change.data).floatValue());
            } else if (change.id == ParamId.NPC_TYPE) {
                npc.setNpcType(newValue);
            } else if (change.id == ParamId.UNKNOWN_34) {
                // ParamId 34 - unknown value (NOT HP)
                log.trace("[UNKNOWN] NPC {} - ParamId 34: {}", npc.getId(), newValue);
            } else if (change.id == ParamId.ENTITY_TYPE) {
                // ParamId 42 - entity type (3 = NPC)
                log.trace("[ENTITY_TYPE] NPC {} - type: {}", npc.getId(), newValue);
            } else {
                // Log other parameter changes at trace level
                log.trace("[OTHER PARAM] NPC {} - ParamId {}: value={}", 
                    npc.getId(), change.id, change.data);
            }
        } catch (Exception e) {
            log.error("[NPC {}] Failed to apply change id={}: {} - {}", 
                npc.getId(), change.id, change.data, e.getMessage());
        }
    }

    /**
     * Process other player entity update.
     */
    private void processOtherPlayer(GameStateResponsePacket.ShipInResponse ship, World world) {
        PlayerEntity player = world.getOrCreatePlayer(ship.id);

        // Set enemy status based on relation
        // relation: 0 = neutral, 1 = friendly, 2 = enemy, etc.
        player.setEnemy(ship.relation == 2 || ship.clanRelation == 2);

        for (ChangedParameter change : ship.changes) {
            applyPlayerChange(player, change);
        }

        log.trace("Updated player: {}", player);
    }

    /**
     * Apply a parameter change to other player.
     * Other players use same ParamIds as own player (25/26, not 24/31).
     */
    private void applyPlayerChange(PlayerEntity player, ChangedParameter change) {
        if (change.data == null) return;

        try {
            if (change.id == ParamId.POSITION) {
                float[] pos = parsePosition(change.data);
                if (pos != null) {
                    player.setPosition(pos[0], pos[1]);
                }
            } else if (change.id == ParamId.PLAYER_HP) {
                player.setHp(((Number) change.data).intValue());
            } else if (change.id == ParamId.PLAYER_MAX_HP) {
                player.setMaxHp(((Number) change.data).intValue());
            } else if (change.id == ParamId.PLAYER_SHIELD) {
                player.setShield(((Number) change.data).intValue());
            } else if (change.id == ParamId.PLAYER_MAX_SHIELD) {
                player.setMaxShield(((Number) change.data).intValue());
            }
        } catch (Exception e) {
            log.error("[Player {}] Failed to apply change id={}: {} - {}", 
                player.getId(), change.id, change.data, e.getMessage());
        }
    }

    /**
     * Process own ship state update.
     */
    private void processOwnShip(GameStateResponsePacket.ShipInResponse ship, World world) {
        PlayerState player = world.getPlayer();

        for (ChangedParameter change : ship.changes) {
            applyOwnShipChange(player, change);
        }

        log.trace("Updated own ship: {}", player);
    }

    /**
     * Apply a parameter change to own ship.
     */
    private void applyOwnShipChange(PlayerState player, ChangedParameter change) {
        if (change.data == null) return;

        log.debug("OwnShip param: id={}, data={}", change.id, change.data);

        try {
            if (change.id == ParamId.POSITION) {
                float[] pos = parsePosition(change.data);
                if (pos != null) {
                    player.setPosition(pos[0], pos[1]);
                }
            } else if (change.id == ParamId.PLAYER_HP) {
                int hpValue = toInt(change.data);
                player.setHp(hpValue);
                log.info("Player HP set to: {}/{}", hpValue, player.getMaxHp());
            } else if (change.id == ParamId.HP) {
                // Ignore NPC HP parameter for player's own ship
                log.warn("Received NPC HP (id=24) for OwnShip with value: {} - IGNORING", change.data);
            } else if (change.id == ParamId.PLAYER_MAX_HP) {
                int maxHpValue = toInt(change.data);
                player.setMaxHp(maxHpValue);
                log.info("Player MAX_HP set to: {}/{}", player.getHp(), maxHpValue);
            } else if (change.id == ParamId.MAX_HP) {
                // Ignore NPC MAX_HP parameter for player's own ship
                log.warn("Received NPC MAX_HP (id=31) for OwnShip with value: {} - IGNORING", change.data);
            } else if (change.id == ParamId.PLAYER_SHIELD) {
                player.setShield(toInt(change.data));
                log.info("Player Shield: {}/{}", player.getShield(), player.getMaxShield());
            } else if (change.id == ParamId.PLAYER_MAX_SHIELD) {
                player.setMaxShield(toInt(change.data));
                log.info("Player Max Shield: {}/{}", player.getShield(), player.getMaxShield());
            } else if (change.id == ParamId.SPEED) {
                player.setSpeed(toFloat(change.data));
            } else if (change.id == ParamId.CARGO_USED) {
                player.setCargoUsed(toInt(change.data));
            } else if (change.id == ParamId.CARGO_MAX) {
                player.setCargoMax(toInt(change.data));
            }
        } catch (Exception e) {
            log.trace("Failed to apply change id={}: {} ({})", change.id, change.data, change.data.getClass().getSimpleName());
        }
    }

    private int toInt(Object data) {
        if (data instanceof Number) {
            return ((Number) data).intValue();
        }
        return 0;
    }

    private float toFloat(Object data) {
        if (data instanceof Number) {
            return ((Number) data).floatValue();
        }
        return 0f;
    }

    /**
     * Safely parse POSITION data which can be int[], float[], or Number[].
     * Returns [x, y] as floats, or null if parsing fails.
     */
    private float[] parsePosition(Object data) {
        if (data == null) return null;
        
        try {
            if (data instanceof int[] intArr) {
                if (intArr.length >= 2) {
                    return new float[]{intArr[0], intArr[1]};
                }
            } else if (data instanceof float[] floatArr) {
                if (floatArr.length >= 2) {
                    return new float[]{floatArr[0], floatArr[1]};
                }
            } else if (data instanceof double[] doubleArr) {
                if (doubleArr.length >= 2) {
                    return new float[]{(float) doubleArr[0], (float) doubleArr[1]};
                }
            } else if (data instanceof Number[] numArr) {
                if (numArr.length >= 2) {
                    return new float[]{numArr[0].floatValue(), numArr[1].floatValue()};
                }
            } else if (data instanceof Object[] objArr) {
                if (objArr.length >= 2 && objArr[0] instanceof Number && objArr[1] instanceof Number) {
                    return new float[]{((Number) objArr[0]).floatValue(), ((Number) objArr[1]).floatValue()};
                }
            }
            log.warn("Unknown POSITION format: {} (type={})", data, data.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to parse POSITION: {} (type={}) - {}", data, data.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * Process MapInfoPacket - map change.
     */
    private void processMapInfo(MapInfoPacket packet, World world) {
        // Track current map for NPC discovery
        currentMapId = packet.mapId;

        MapInfo map = world.getMapInfo();
        map.setMapId(packet.mapId);
        map.setMapName(packet.name);
        map.setSize(packet.width, packet.height);

        // Set safe zone position - priority: space station > nearest portal
        log.info("MapInfo: spaceStation={}, ssx={}, ssy={}, teleports={}", 
                 packet.spaceStation, packet.ssx, packet.ssy, 
                 packet.teleports != null ? packet.teleports.length : 0);
        
        if (packet.spaceStation && (packet.ssx > 0 || packet.ssy > 0)) {
            // Space station is the safe zone
            map.setSafeZone(packet.ssx, packet.ssy, 500f);
            log.info("Safe zone set to space station: ({}, {})", packet.ssx, packet.ssy);
        } else if (packet.teleports != null && packet.teleports.length > 0) {
            // Use first portal as safe zone (portals are safe zones on allied maps)
            // In the future, could find nearest portal to player position
            MapInfoPacket.TPort firstPortal = packet.teleports[0];
            map.setSafeZone(firstPortal.x, firstPortal.y, 300f);
            log.info("Safe zone set to portal: ({}, {})", firstPortal.x, firstPortal.y);
        } else {
            // No safe zone available - use map center as fallback
            map.setSafeZone(packet.width / 2f, packet.height / 2f, 0f);
            log.warn("No safe zone found, using map center: ({}, {})", packet.width / 2f, packet.height / 2f);
        }

        // Clear entities on map change
        world.clearAll();

        log.info("Map changed to: {} (id={})", packet.name, packet.mapId);
    }

    /**
     * Process CollectableInPacket - box spawn/despawn.
     */
    private void processCollectable(CollectableInPacket packet, World world) {
        if (!packet.existOnMap) {
            // Box removed from map
            world.removeBox(packet.id);
            log.trace("Box {} removed", packet.id);
            return;
        }

        // Box spawned or updated
        BoxEntity box = world.getOrCreateBox(packet.id);
        box.setBoxType(packet.type);
        box.setPosition(packet.x, packet.y);

        log.trace("Box updated: {}", box);
    }

    /**
     * Process UserInfoResponsePacket - detailed player info.
     */
    private void processUserInfo(UserInfoResponsePacket packet, World world) {
        if (packet.params == null) return;

        PlayerState player = world.getPlayer();

        for (ChangedParameter param : packet.params) {
            applyOwnShipChange(player, param);
        }

        log.debug("User info updated: {}", player);
    }

    /**
     * Process EquipResponsePacket - hangar/ship data.
     */
    private void processEquipResponse(EquipResponsePacket packet) {
        if (shipManager != null && packet.hangars != null) {
            shipManager.updateShips(packet);
            log.info("Hangars updated: {}", shipManager.getCargoSummary());
        }
    }

    /**
     * Process MessageResponsePacket - server messages.
     */
    private void processMessage(MessageResponsePacket packet) {
        log.debug("Server message [{}]: {}", packet.msgId, packet.msg);
    }

    /**
     * Get current player ID.
     */
    public int getPlayerId() {
        return playerId;
    }
}
