/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.chat;

public class ChatMessageData {
    public String roomId;
    public String author;
    public String msg;
    public int status;

    public ChatMessageData(String string, String string2, String string3, int n) {
        this.roomId = string;
        this.author = string2;
        this.msg = string3;
        this.status = n;
    }

    public Object toStringArray() {
        return new String[]{this.roomId, this.author, this.msg, String.valueOf(this.status)};
    }

    public static ChatMessageData parse(Object object) {
        String[] stringArray = (String[])object;
        return new ChatMessageData(stringArray[0], stringArray[1], stringArray[2], Integer.parseInt(stringArray[3]));
    }
}
