/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class ChangedParameter {
    public int id;
    public int type;
    public Object data;

    public ChangedParameter(int n, Object object) {
        this(n, 0, object);
    }

    public ChangedParameter(int n, int n2, Object object) {
        this.id = n;
        this.type = n2;
        this.data = object;
    }

    public ChangedParameter() {
    }

    public boolean isChanged(Object object) {
        return this.data == null && object != null || this.data != null && !this.data.equals(object);
    }

    @Override
    public String toString() {
        String dataStr;
        if (data == null) {
            dataStr = "null";
        } else if (data instanceof int[]) {
            dataStr = java.util.Arrays.toString((int[]) data);
        } else if (data instanceof float[]) {
            dataStr = java.util.Arrays.toString((float[]) data);
        } else if (data instanceof Object[]) {
            dataStr = java.util.Arrays.toString((Object[]) data);
        } else {
            dataStr = String.valueOf(data);
        }
        return String.format("{id=%d, type=%d, data=%s}", id, type, dataStr);
    }
}
