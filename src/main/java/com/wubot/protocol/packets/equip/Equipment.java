/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

public class Equipment {
    public static final int LASER = 1;
    public static final int SPEEDGEN = 2;
    public static final int SHIELDGEN = 3;
    public static final int EXTENSION = 4;
    public static final int DRONE_COVER = 5;
    public int subtype;
    public int type;
    public int id;
    public int price;
    public int sellPrice;
    public boolean elite;

    public Equipment() {
        this.type = 0;
    }

    public Equipment(int n) {
        this.type = n;
    }

    public String toString() {
        return "Equipment{id=" + this.id + ", type=" + this.type + ", subtype=" + this.subtype + '}';
    }
}
