/*
 * Decompiled with CFR 0.152.
 */
package com.wubot.protocol.packets;

public class GameStateResponsePacket {
    @Deprecated
    public ChangedParameter[] mapChanges;
    public GameEvent[] events;
    public MapEvent[] mapEvents;
    public boolean flushCollectables;
    public CollectableInPacket[] collectables;
    public int playerId;
    public int confi;
    public boolean safeZone;
    public ShipInResponse[] ships;

    public static class ShipInResponse {
        public int id;
        public ChangedParameter[] changes;
        public boolean mrs;
        public int clanRelation;
        public int relation;
        public boolean posImportant;
        public boolean destroyed;
        public int[] damages;
        public int[] restores;

        @Override
        public String toString() {
            return String.format("{id=%d, relation=%d, clanRel=%d, changes=%s, damages=%s, restores=%s, destroyed=%s}",
                id, relation, clanRelation,
                changes != null ? java.util.Arrays.toString(changes) : "null",
                damages != null ? java.util.Arrays.toString(damages) : "null",
                restores != null ? java.util.Arrays.toString(restores) : "null",
                destroyed);
        }
    }

    @Override
    public String toString() {
        return String.format("GameStateResponse{playerId=%d, safeZone=%s, ships=%s, collectables=%s, events=%s}",
            playerId, safeZone,
            ships != null ? java.util.Arrays.toString(ships) : "null",
            collectables != null ? java.util.Arrays.toString(collectables) : "null",
            events != null ? java.util.Arrays.toString(events) : "null");
    }
}
