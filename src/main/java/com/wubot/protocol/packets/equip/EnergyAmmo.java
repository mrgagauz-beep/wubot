/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Ammo;

public class EnergyAmmo
extends Ammo {
    public static final int ELECTRIC_ENERGY = 1;
    public static final int NUCLEAR_ENERGY = 2;
    public static final int MAGNETIC_ENERGY = 3;
    public static final int GRAVITY_ENERGY = 4;

    public EnergyAmmo() {
        super(1);
    }

    public EnergyAmmo(int n) {
        super(3);
        this.subtype = n;
        this.elite = true;
        this.price = n == 1 ? 50 : (n == 2 ? 250 : (n == 3 ? 150 : (n == 4 ? 200 : 50)));
    }
}
