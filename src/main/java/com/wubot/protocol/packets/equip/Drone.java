/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.DroneCover;
import com.wubot.protocol.packets.equip.Equipment;

public class Drone {
    public static final int ARES = 1;
    public static final int NIMBUS = 2;
    public int type;
    public int id;
    public int slots;
    public int price;
    public boolean elite;
    public Equipment[] equip;
    public DroneCover cover;
}
