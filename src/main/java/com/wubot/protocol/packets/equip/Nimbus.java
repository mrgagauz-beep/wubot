/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Drone;
import com.wubot.protocol.packets.equip.Equipment;

public class Nimbus
extends Drone {
    public static int BASE_PRICE = 100000;

    public Nimbus() {
        this.type = 2;
        this.price = BASE_PRICE;
        this.elite = false;
        this.slots = 1;
        this.equip = new Equipment[this.slots];
    }
}
