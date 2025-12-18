/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class MapEvent {
    private static int n = 0;
    public static final int MOTRON_BOMB_EXPLOSION = n++;
    public static final int NBOMB_EXPLOSION = n++;
    public static final int EMP_EXPLOSION = n++;
    public static final int MINE_EXPLOSION = n++;
    public int type;
    public int x;
    public int y;
    public Object data;

    public MapEvent() {
    }

    public MapEvent(int n, int n2, int n3) {
        this.type = n;
        this.x = n2;
        this.y = n3;
    }

    public MapEvent(int n, float f, float f2) {
        this.type = n;
        this.x = (int)f;
        this.y = (int)f2;
    }

    @Override
    public String toString() {
        return String.format("{type=%d, pos=(%d,%d), data=%s}", type, x, y, data);
    }
}
