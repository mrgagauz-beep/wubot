/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Equipment;

public class Extension
extends Equipment {
    public static final int NB_MODULE = 1;
    public static final int WS_MODULE = 2;
    public static final int EMP_MODULE = 3;
    public static final int INVIS_MODULE = 4;
    public static final int FAST_REPAIR = 5;
    public static final int ENERGY_TRANSFER = 6;
    public static final int AUTOMATIC_COMPRESSOR = 7;
    public int actionTime;
    public int cooldown;
    public int eEnergyUse;
    public int mEnergyUse;
    public int gEnergyUse;
    public int nEnergyUse;

    public Extension() {
        super(1);
    }

    public Extension(int n) {
        super(4);
        this.subtype = n;
        this.elite = true;
        if (n == 1) {
            this.price = 90000;
            this.sellPrice = 2250000;
            this.actionTime = 0;
            this.cooldown = 30;
            this.eEnergyUse = 5;
            this.nEnergyUse = 1;
        } else if (n == 2) {
            this.price = 120000;
            this.sellPrice = 3000000;
            this.actionTime = 3;
            this.cooldown = 25;
            this.eEnergyUse = 4;
            this.gEnergyUse = 1;
        } else if (n == 3) {
            this.price = 150000;
            this.sellPrice = 3750000;
            this.actionTime = 3;
            this.cooldown = 40;
            this.eEnergyUse = 3;
            this.mEnergyUse = 1;
        } else if (n == 4) {
            this.price = 120000;
            this.sellPrice = 3000000;
            this.actionTime = 60;
            this.cooldown = 240;
            this.eEnergyUse = 10;
        } else if (n == 5) {
            this.price = 85000;
            this.sellPrice = 2125000;
            this.actionTime = 10;
            this.cooldown = 120;
            this.eEnergyUse = 20;
        } else if (n == 6) {
            this.price = 95000;
            this.sellPrice = 2375000;
            this.actionTime = 300;
            this.cooldown = 120;
            this.eEnergyUse = 30;
        } else if (n == 7) {
            this.price = 15000;
            this.sellPrice = 375000;
        }
    }
}
