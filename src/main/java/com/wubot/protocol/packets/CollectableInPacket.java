/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class CollectableInPacket {
    public int id;
    public int type;
    public int subtype;
    public boolean existOnMap;
    public int x;
    public int y;

    @Override
    public String toString() {
        return String.format("{id=%d, type=%d, subtype=%d, pos=(%d,%d), exists=%s}",
            id, type, subtype, x, y, existOnMap);
    }
}
