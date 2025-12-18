/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class MapInfoPacket {
    public int mapId;
    public String name;
    public int width;
    public int height;
    public boolean spaceStation;
    public float ssx;
    public float ssy;
    public int[] tradeStation;
    public TPort[] teleports;

    public static class TPort {
        public int type;
        public int subtype;
        public int x;
        public int y;
    }
}
