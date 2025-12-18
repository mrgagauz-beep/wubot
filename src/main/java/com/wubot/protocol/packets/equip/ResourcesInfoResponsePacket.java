/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets.equip;

public class ResourcesInfoResponsePacket {
    public ResourceInfo[] resources;
    public EnrichmentInfo[] enriches;

    public static class EnrichmentInfo {
        public int amount;
        public int type;
        public int[] possibleResources;
    }
}
