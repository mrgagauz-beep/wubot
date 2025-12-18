/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class ResourcesPack {
    // 8 resource types in game
    private static final int RESOURCE_COUNT = 8;
    private int[] amounts = new int[RESOURCE_COUNT];

    public void set(int n, int n2) {
        this.amounts[n] = n2;
    }

    public int get(int n) {
        return this.amounts[n];
    }

    public int[] getRaw() {
        return this.amounts;
    }

    public boolean isEmpty() {
        for (int n : this.amounts) {
            if (n <= 0) continue;
            return false;
        }
        return true;
    }
}
