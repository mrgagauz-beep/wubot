/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Ammo;

public class MineAmmo
extends Ammo {
    public static final int TYPE_1 = 1;
    public static final int TYPE_2 = 2;
    public static final int TYPE_3 = 3;
    public int damage;
    public int distance;

    public MineAmmo() {
        super(4);
    }

    public MineAmmo(int n) {
        super(4);
        this.subtype = n;
        if (n == 1) {
            this.price = 100;
            this.distance = 500;
            this.damage = 6000;
        }
    }
}
