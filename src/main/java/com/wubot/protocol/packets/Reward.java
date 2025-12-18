/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

import com.wubot.protocol.packets.RewardItem;
import java.util.Arrays;

public class Reward {
    static int n = 0;
    public static final int btc = n++;
    public static final int plt = n++;
    public static final int exp = n++;
    public static final int hnr = n++;
    public static final int lammo = n++;
    public static final int eammo = n++;
    public static final int rammo = n++;
    public static final int miner_ticket = n++;
    public static final int premium = n++;
    public static final int GUN = n++;
    public static final int SHIELDGEN = n++;
    public static final int SPEEDGEN = n++;
    public static final int DRONE_COVER = n++;
    public static final int EXTENSION = n++;
    public static final int DRONE = n++;
    public static final int GOLD = n++;
    RewardItem[] items;

    public void addReward(RewardItem rewardItem) {
        this.addReward(rewardItem.type, rewardItem.subtype, rewardItem.amount);
    }

    public void addReward(int n, int n2) {
        this.addReward(n, 0, n2);
    }

    public void addReward(int n, int n2, int n3) {
        if (n3 == 0) {
            return;
        }
        this.items = this.items == null ? new RewardItem[1] : Arrays.copyOf(this.items, this.items.length + 1);
        RewardItem rewardItem = new RewardItem();
        rewardItem.type = n;
        rewardItem.subtype = n2;
        rewardItem.amount = n3;
        this.items[this.items.length - 1] = rewardItem;
    }

    public RewardItem[] getItems() {
        return this.items;
    }

    public RewardItem getItem(int n) {
        return this.getItem(n, 0);
    }

    public RewardItem getItem(int n, int n2) {
        if (this.items == null) {
            return null;
        }
        for (RewardItem rewardItem : this.getItems()) {
            if (rewardItem.type != n || rewardItem.subtype != n2) continue;
            return rewardItem;
        }
        return null;
    }

    public void multiply(int n, float f) {
        this.multiply(n, 0, f);
    }

    public void multiply(int n, int n2, float f) {
        RewardItem rewardItem = this.getItem(n, n2);
        if (rewardItem != null) {
            rewardItem.amount = (int)((float)rewardItem.amount * f);
        }
    }

    public Reward multiply(float f) {
        for (RewardItem rewardItem : this.getItems()) {
            rewardItem.amount = (int)((float)rewardItem.amount * f);
        }
        return this;
    }
}
