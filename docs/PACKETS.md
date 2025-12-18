# WarUniverse Bot - Протокол пакетов

## Исходящие пакеты (Client → Server)

### UserActionsPacket

```java
public class UserActionsPacket {
    public int actionId;
    public String value;
}
```

| actionId | Действие | value | Пример |
|----------|----------|-------|--------|
| 1 | MOVE | "x\|y" | "1234.5678\|567.0" |
| 2 | LOCK | "id" | "12345" |
| 3 | ATTACK | "" | "" |
| 4 | STOP_ATTACK | "" | "" |

**ВАЖНО:** Координаты форматировать с `Locale.US`!

```java
String.format(Locale.US, "%.4f|%.1f", x, y)
```

### CollectableCollectRequest

```java
public class CollectableCollectRequest {
    public int id;  // ID бокса
}
```

### TeleportRequest

```java
public class TeleportRequest {
    public int portalId;
}
```

---

## Входящие пакеты (Server → Client)

### MapInfoPacket

```java
public class MapInfoPacket {
    public int mapId;
    public String mapName;
    public int width, height;
    public boolean hasSpaceStation;
    public float spaceStationX, spaceStationY;
    public TeleportInfo[] teleports;  // Порталы на карте
}

public class TeleportInfo {
    public int id;
    public float x, y;
    public int type;
    public int subtype;
}
```

### GameStateResponsePacket

```java
public class GameStateResponsePacket {
    public int playerId;
    public boolean safeZone;
    public int confi;  // ID конфигурации
    public Ship[] ships;
    public Collectable[] collectables;
}

public class Ship {
    public int id;
    public int npcId;      // 0 = игрок, >0 = NPC type
    public float x, y;
    public Change[] changes;
}

public class Change {
    public int paramId;
    public Object value;
}

public class Collectable {
    public int id;
    public int type;
    public float x, y;
}
```

### Parameter IDs (Change.paramId)

| ID | Параметр | Тип значения |
|----|----------|--------------|
| 9 | NPC_TYPE | int |
| 17 | POSITION | int[] {x, y} |
| 25 | HP | int |
| 26 | MAX_HP | int |
| 27 | SHIELD | int |
| 28 | MAX_SHIELD | int |
| 31 | SPEED | float |
| 32 | CARGO_USED | int |
| 33 | CARGO_MAX | int |

### CollectableInPacket

```java
public class CollectableInPacket {
    public int id;
    public int type;
    public float x, y;
    public boolean existOnMap;  // false = удалён
}
```

### MessageResponsePacket

```java
public class MessageResponsePacket {
    public int type;
    public String message;
}
```

| type | Значение |
|------|----------|
| 1 | NPC_KILLED |
| 2 | PLAYER_KILLED |
| 3 | BOX_COLLECTED |
| 4 | DAMAGE_RECEIVED |

---

## Типы боксов (Collectable.type)

| type | Описание | Собирать? |
|------|----------|-----------|
| 1 | BONUS_BOX | ✅ Всегда |
| 2 | RESOURCE_1 | ❌ Игнорировать |
| 3 | RESOURCE_2 | ❌ Игнорировать |
| 4 | LOOT | ✅ Если карго не полное |
| 13 | SPECIAL_1 | ❌ Игнорировать |
| 14 | SPECIAL_2 | ❌ Игнорировать |

---

## Протокол атаки

```
1. LOCK (однократно)      → UserActionsPacket(2, "targetId")
2. ATTACK (однократно)    → UserActionsPacket(3, "")
3. Атака продолжается автоматически
4. MOVE не прерывает атаку
5. При смене цели: LOCK нового → ATTACK
```

---

## Протокол сбора

```
1. MOVE to (box.x, box.y + 97)     // Отправить пакет движения
2. ЖДАТЬ прибытия (~700-900ms)  // ОБЯЗАТЕЛЬНО!
3. CollectableCollectRequest(boxId) // ТОЛЬКО ПОСЛЕ прилёта
```

**ВАЖНО:**
- Y_OFFSET = 97 обязателен!
- MOVE и COLLECT отправляются **ПОСЛЕДОВАТЕЛЬНО**, не одновременно!
- Быстрый сбор без движения/ожидания НЕ работает!

---

## Протокол телепортации

```
1. MOVE к порталу
2. При distance < 100: TeleportRequest(portalId)
3. Сервер отправит MapInfoPacket новой карты
```

---

## Rate Limiting

- Минимальный интервал между пакетами: 50ms
- MOVE можно отправлять чаще (каждый tick)
- LOCK/ATTACK — только при необходимости

---

## Пример обработки GameStateResponsePacket

```java
void processGameState(GameStateResponsePacket packet, World world) {
    world.player().setSafeZone(packet.safeZone);
    
    for (Ship ship : packet.ships) {
        if (ship.npcId > 0) {
            // NPC
            processNpc(ship, world);
        } else if (ship.id != packet.playerId) {
            // Другой игрок
            processPlayer(ship, world);
        } else {
            // Наш корабль
            processOwnShip(ship, world);
        }
    }
    
    for (Collectable c : packet.collectables) {
        world.boxes().update(c.id, c.type, c.x, c.y);
    }
}

void processNpc(Ship ship, World world) {
    NpcEntity npc = world.npcs().getOrCreate(ship.id);
    
    for (Change change : ship.changes) {
        switch (change.paramId) {
            case 17 -> {  // POSITION
                int[] pos = (int[]) change.value;
                npc.setPosition(pos[0], pos[1]);
            }
            case 25 -> npc.setHp((int) change.value);
            case 26 -> npc.setMaxHp((int) change.value);
            case 31 -> npc.setSpeed((float) change.value);
            case 9 -> npc.setNpcType((int) change.value);
        }
    }
}
```
