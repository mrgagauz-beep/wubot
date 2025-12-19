package com.wubot.network;

import com.wubot.protocol.packets.CollectableCollectRequest;
import com.wubot.protocol.packets.RepairRequestPacket;
import com.wubot.protocol.packets.TeleportRequestPacket;
import com.wubot.protocol.packets.UserActionsPacket;
import com.wubot.protocol.packets.UserActionsPacket.UserAction;
import com.wubot.protocol.packets.equip.EquipHangarActionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Packet sender with rate limiting and helper methods.
 * Wraps Connection and provides convenient methods for game actions.
 */
public class PacketSender {
    private static final Logger log = LoggerFactory.getLogger(PacketSender.class);

    private static final long MIN_INTERVAL_MS = 50;

    private final Connection connection;
    private long lastSendTime = 0;

    public PacketSender(Connection connection) {
        this.connection = connection;
    }

    /**
     * Send packet with rate limiting.
     */
    public void send(Object packet) {
        enforceRateLimit();
        connection.send(packet);
        lastSendTime = System.currentTimeMillis();
    }

    /**
     * Enforce minimum interval between packets.
     */
    private void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSendTime;
        if (elapsed < MIN_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_INTERVAL_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // === Helper methods ===

    /**
     * Send move command.
     * IMPORTANT: Coordinates must be formatted with Locale.US!
     *
     * @param x target X coordinate
     * @param y target Y coordinate
     */
    public void move(float x, float y) {
        UserActionsPacket packet = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.MOVE;
        action.data = String.format(Locale.US, "%.4f|%.1f", x, y);
        packet.actions = new UserAction[]{action};
        send(packet);
        log.debug("MOVE to ({}, {})", x, y);
    }

    /**
     * Send lock target command.
     *
     * @param targetId ID of target to lock
     */
    public void lock(int targetId) {
        UserActionsPacket packet = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.LOCK;
        action.data = String.valueOf(targetId);
        packet.actions = new UserAction[]{action};
        send(packet);
        log.debug("LOCK target {}", targetId);
    }

    /**
     * Send attack command.
     * Must lock target first!
     */
    public void attack() {
        UserActionsPacket packet = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.ATTACK;
        action.data = "";
        packet.actions = new UserAction[]{action};
        send(packet);
        log.debug("ATTACK");
    }

    /**
     * Send stop attack command.
     */
    public void stopAttack() {
        UserActionsPacket packet = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.STOP_ATTACK;
        action.data = "";
        packet.actions = new UserAction[]{action};
        send(packet);
        log.debug("STOP_ATTACK");
    }

    /**
     * Send collect box command.
     * IMPORTANT: Must be at box position first!
     *
     * @param boxId ID of box to collect
     */
    public void collect(int boxId) {
        CollectableCollectRequest packet = new CollectableCollectRequest();
        packet.id = boxId;
        send(packet);
        log.debug("COLLECT box {}", boxId);
    }

    /**
     * Send teleport request.
     * Must be near portal (distance < 100)!
     * 
     * Protocol: Send UserActionsPacket with actionId=TELEPORT, then TeleportRequestPacket with portal ID.
     * Server determines which portal based on player position and portal ID.
     * 
     * @param portalId ID of portal to teleport through (from TPort.id in MapInfoPacket)
     */
    public void teleport(int portalId) {
        // Step 1: Send UserActionsPacket with TELEPORT action
        UserActionsPacket actionPacket = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.TELEPORT;
        action.data = "";  // No data needed for teleport action
        actionPacket.actions = new UserAction[]{action};
        send(actionPacket);
        log.info("TELEPORT action sent via UserActionsPacket");
        
        // Step 2: Send TeleportRequestPacket with portal ID
        TeleportRequestPacket teleportPacket = new TeleportRequestPacket(portalId);
        send(teleportPacket);
        log.info("TELEPORT request sent for portal ID {}", portalId);
    }

    /**
     * Send switch configuration command.
     *
     * @param configId configuration ID (1 or 2)
     */
    public void switchConfig(int configId) {
        UserActionsPacket packet = new UserActionsPacket();
        UserAction action = new UserAction();
        action.actionId = UserAction.SWITCH_CONFI;
        action.data = String.valueOf(configId);
        packet.actions = new UserAction[]{action};
        send(packet);
        log.debug("SWITCH_CONFIG to {}", configId);
    }

    /**
     * Send switch ship (hangar) command.
     * Must be in safe zone!
     *
     * @param hangarId ID of hangar to switch to
     */
    public void switchShip(int hangarId) {
        // Action ID 1 = activate hangar
        EquipHangarActionRequest packet = new EquipHangarActionRequest();
        packet.actionId = 1;
        packet.data = hangarId;
        send(packet);
        log.debug("SWITCH_SHIP to hangar {}", hangarId);
    }

    /**
     * Send repair request.
     * Used when ship is destroyed to respawn.
     */
    public void repair() {
        RepairRequestPacket packet = new RepairRequestPacket();
        send(packet);
        log.info("REPAIR request sent");
    }

    /**
     * Check if connection is active.
     */
    public boolean isConnected() {
        return connection.isConnected();
    }
}
