/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.HangarInPacket;
import com.wubot.protocol.packets.equip.Drone;
import com.wubot.protocol.packets.equip.Equipment;

public class EquipResponsePacket {
    public HangarInPacket[] hangars;
    public int hangarPrice;
    public Equipment[] onShip;
    public Equipment[] equip;
    public Drone[] drones;
    public int laserSlots;
    public int genSlots;
    public int extSlots;
}
