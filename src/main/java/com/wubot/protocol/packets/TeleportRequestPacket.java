/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

/**
 * Request to teleport through a portal.
 * Send when player is near portal (distance < 100).
 */
public class TeleportRequestPacket {
    /** Portal ID to teleport through */
    public int portalId;
    
    public TeleportRequestPacket() {
    }
    
    public TeleportRequestPacket(int portalId) {
        this.portalId = portalId;
    }
}
