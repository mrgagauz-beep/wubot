/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

public class Ammo {
    public static final int LASER = 1;
    public static final int ROCKET = 2;
    public static final int ENERGY = 3;
    public static final int MINE = 4;
    public int subtype;
    public int type;
    public int price;
    public int sellPrice;
    public boolean elite;
    public int quantity;

    public Ammo() {
        this.type = 0;
    }

    public Ammo(int n) {
        this.type = n;
    }
}
