/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Drone;
import com.wubot.protocol.packets.equip.Equipment;

public class Ares
extends Drone {
    public static int BASE_PRICE = 12000;

    public Ares() {
        this.type = 1;
        this.price = BASE_PRICE;
        this.elite = true;
        this.slots = 2;
        this.equip = new Equipment[this.slots];
    }
}
