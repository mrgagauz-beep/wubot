/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.shop;

import com.wubot.protocol.packets.ShipInPacket;
import com.wubot.protocol.packets.equip.Ammo;
import com.wubot.protocol.packets.equip.Drone;
import com.wubot.protocol.packets.equip.Equipment;

@Deprecated
public class ShopItemsResponsePacket {
    @Deprecated
    public Equipment[] items;
    @Deprecated
    public ShipInPacket[] ships;
    @Deprecated
    public Drone[] drones;
    @Deprecated
    public Ammo[] ammo;
}
