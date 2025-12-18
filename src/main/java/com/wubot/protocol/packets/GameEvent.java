/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class GameEvent {
    public int id;
    public Object data;

    @Override
    public String toString() {
        String dataStr;
        if (data == null) {
            dataStr = "null";
        } else if (data instanceof int[]) {
            dataStr = java.util.Arrays.toString((int[]) data);
        } else if (data instanceof Object[]) {
            dataStr = java.util.Arrays.deepToString((Object[]) data);
        } else {
            dataStr = String.valueOf(data);
        }
        return String.format("{id=%d, data=%s}", id, dataStr);
    }
}
