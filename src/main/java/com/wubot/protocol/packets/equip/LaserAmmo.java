/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Ammo;

public class LaserAmmo
extends Ammo {
    public static final int LASER_RED = 1;
    public static final int LASER_GREEN = 2;
    public static final int LASER_BLUE = 3;
    public static final int LASER_WHITE = 4;
    public static final int LASER_ASHIELD = 5;
    public static final int LASER_MRS = 6;
    public int multi;

    public LaserAmmo() {
        super(1);
    }

    public LaserAmmo(int n) {
        super(1);
        this.subtype = n;
        if (n == 1) {
            this.price = 100;
            this.multi = 1;
        } else if (n == 2) {
            this.price = 4000;
            this.multi = 2;
        } else {
            this.elite = true;
            if (n == 3) {
                this.price = 10;
                this.multi = 3;
            } else if (n == 4) {
                this.price = 1000;
                this.multi = 4;
            } else if (n == 5) {
                this.price = 10;
                this.multi = 3;
            } else if (n == 6) {
                this.price = 50;
                this.multi = 6;
            } else {
                this.price = 10;
                this.multi = 1;
            }
        }
    }
}
