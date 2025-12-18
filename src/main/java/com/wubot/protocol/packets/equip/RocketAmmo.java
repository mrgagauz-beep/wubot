/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Ammo;

public class RocketAmmo
extends Ammo {
    public static final int TYPE_1 = 1;
    public static final int TYPE_2 = 2;
    public static final int TYPE_3 = 3;
    public int distance;
    public int damage;
    public int speed = 1000;

    public RocketAmmo() {
        super(2);
    }

    public RocketAmmo(int n) {
        super(2);
        this.subtype = n;
        if (n == 1) {
            this.price = 100;
            this.distance = 600;
            this.damage = 1000;
        } else if (n == 2) {
            this.price = 500;
            this.distance = 700;
            this.damage = 2000;
        } else if (n == 3) {
            this.elite = true;
            this.price = 5;
            this.distance = 800;
            this.damage = 4000;
        } else {
            this.price = 100;
        }
    }
}
