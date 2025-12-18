/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

import com.wubot.protocol.packets.equip.Equipment;

public class DroneCover
extends Equipment {
    public static final int COVER_1 = 1;
    public static final int COVER_2 = 2;
    public static final int COVER_3 = 3;
    @Deprecated
    int damageBoost;
    @Deprecated
    int shieldBoost;

    public DroneCover() {
    }

    public DroneCover(int n) {
        super(5);
        this.subtype = n;
    }
}
