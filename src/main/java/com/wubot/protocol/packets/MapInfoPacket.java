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
        // NOTE: Keep id field for Kryo deserialization compatibility even if not used
        // Server may send id field even if docs don't mention it
        public int id;         // Portal ID (for Kryo compatibility)
        public int type;       // Portal type
        public int subtype;    // Subtype (target map ID?)
        public int x;          // X coordinate
        public int y;          // Y coordinate
    }
}
