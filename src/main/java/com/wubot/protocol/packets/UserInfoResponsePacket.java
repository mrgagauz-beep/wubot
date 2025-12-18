/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

import com.wubot.protocol.packets.ChangedParameter;

public class UserInfoResponsePacket {
    public ChangedParameter[] params;

    @Override
    public String toString() {
        return "UserInfoResponse{params=" + java.util.Arrays.toString(params) + "}";
    }
}
