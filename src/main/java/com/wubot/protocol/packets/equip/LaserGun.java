/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Equipment;

public class LaserGun
extends Equipment {
    public static final int LC1_LASER = 1;
    public static final int LC2_LASER = 2;
    public static final int LC3_LASER = 3;
    public static final int LC4_LASER = 4;
    public static final int LC5_LASER = 5;
    public static final int LC6_LASER = 6;
    public static final int LC7_LASER = 7;
    private static final int LC1_LASER_DAMAGE = 60;
    private static final int LC1_LASER_PRICE = 40000;
    private static final int LC2_LASER_DAMAGE = 100;
    private static final int LC2_LASER_PRICE = 250000;
    private static final int LC3_LASER_DAMAGE = 150;
    private static final int LC3_LASER_PRICE = 20000;
    private static final int LC7_LASER_DAMAGE = 175;
    public int damage;

    public LaserGun() {
    }

    public LaserGun(int n) {
        super(1);
        this.subtype = n;
        switch (n) {
            case 1: {
                this.damage = 60;
                this.price = 40000;
                this.sellPrice = 20000;
                break;
            }
            case 2: {
                this.damage = 100;
                this.price = 250000;
                this.sellPrice = 125000;
                break;
            }
            case 3: {
                this.damage = 150;
                this.price = 20000;
                this.sellPrice = 500000;
                this.elite = true;
                break;
            }
            case 4: 
            case 5: {
                this.damage = 150;
                this.price = 40000;
                this.elite = true;
                break;
            }
            case 7: {
                this.damage = 175;
                this.sellPrice = 500000;
                this.elite = true;
            }
        }
    }
}
