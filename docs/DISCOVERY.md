# WarUniverse Bot - Discovery System

## Обзор

Discovery System автоматически собирает информацию о мире во время игры:
- Карты и их характеристики
- Порталы и связи между картами
- Типы NPC и их характеристики
- Точки спавна

Данные сохраняются в JSON и используются для принятия решений.

---

## Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                     PacketProcessor                              │
│                            │                                     │
│                            ▼                                     │
│                   DiscoveryCollector                             │
│                            │                                     │
│         ┌──────────────────┼──────────────────┐                 │
│         ▼                  ▼                  ▼                 │
│   MapRegistry        PortalGraph        NpcDatabase             │
│         │                  │                  │                 │
│         └──────────────────┼──────────────────┘                 │
│                            ▼                                     │
│                   PersistenceManager                             │
│                            │                                     │
│                            ▼                                     │
│                       JSON Files                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Компоненты

### MapRegistry

Хранит информацию о картах.

```java
public class MapRegistry {
    private final Map<Integer, MapData> maps = new ConcurrentHashMap<>();
    
    public static class MapData {
        public int id;
        public String name;
        public int width, height;
        public boolean hasSpaceStation;
        public float spaceStationX, spaceStationY;
        public long firstSeen;
        public long lastVisit;
        public int visitCount;
    }
    
    // Вызывается при получении MapInfoPacket
    public void discover(MapInfoPacket packet) {
        MapData existing = maps.get(packet.mapId);
        
        if (existing == null) {
            // Новая карта!
            MapData newMap = createFromPacket(packet);
            maps.put(packet.mapId, newMap);
            log.info("🗺️ NEW MAP: {} (id={})", packet.mapName, packet.mapId);
            save();
        } else {
            existing.lastVisit = System.currentTimeMillis();
            existing.visitCount++;
        }
    }
}
```

**JSON формат (maps.json):**

```json
{
  "1": {
    "id": 1,
    "name": "X-1",
    "width": 20000,
    "height": 15000,
    "hasSpaceStation": true,
    "spaceStationX": 1000,
    "spaceStationY": 1000,
    "firstSeen": 1702500000000,
    "lastVisit": 1702600000000,
    "visitCount": 42
  }
}
```

---

### PortalGraph

Хранит порталы и связи между картами. Умеет строить маршруты.

#### Порталы: большинство двусторонние, но есть исключения!

```
БОЛЬШИНСТВО порталов двусторонние:
1. Карта A, портал P1 → прыжок → Карта B, появляемся НА портале P2
2. Если прыгнуть в P2 → вернёмся на карту A, на портал P1

Это значит:
- Один прыжок = ДВЕ связи (A→B и B→A)
- Портал на котором появились ведёт ОБРАТНО
- Граф связей симметричный
```

#### ИСКЛЮЧЕНИЕ: Односторонний портал X-1 → X-3

```
На картах X-1 есть ОДНОСТОРОННИЙ портал на X-3:

    X-1 ──────► X-3 ◄──────► X-2
         (односторонний)  (двусторонний)

Последовательность:
1. X-1 → прыжок → X-3 (односторонний, обратно на X-1 не попадёшь!)
2. X-3 → прыжок обратно → X-2 (НЕ X-1!)
3. X-2 → прыжок обратно → X-3 (двусторонний)

Это ЕДИНСТВЕННОЕ известное исключение.
Все остальные порталы — двусторонние.
```

#### Визуализация обычного портала (двусторонний)

```
Карта A                     Карта B
┌────────────────┐          ┌────────────────┐
│                │          │                │
│   [Portal P1]══┼══════════┼══[Portal P2]   │
│                │    ↔     │                │
└────────────────┘          └────────────────┘

Прыжок P1 → появление на P2
Прыжок P2 → появление на P1
```

#### Визуализация одностороннего портала (X-1 → X-3)

```
X-1                         X-3                         X-2
┌────────────────┐          ┌────────────────┐          ┌────────────────┐
│                │          │                │          │                │
│   [Portal]─────┼──────────┼─►[Portal]══════┼══════════┼══[Portal]      │
│                │    →     │                │    ↔     │                │
└────────────────┘          └────────────────┘          └────────────────┘
     (только туда)              (двусторонний с X-2)

X-1 → X-3: односторонний (нельзя вернуться на X-1 через этот портал!)
X-3 ↔ X-2: двусторонний
```

```java
public class PortalGraph {
    private final Map<Integer, List<PortalData>> portalsByMap;
    private final Map<String, PortalConnection> connections;
    
    public static class PortalData {
        public int portalId;
        public int mapId;
        public float x, y;
        public int type, subtype;
        public Integer targetMapId;    // null если не знаем
        public Float targetX, targetY;
        public int useCount;
    }
    
    public static class PortalConnection {
        public int fromMapId, fromPortalId;
        public int toMapId;
        public float toX, toY;
    }
    
    // Записать порталы при входе на карту
    public void discoverPortals(int mapId, TeleportInfo[] teleports);
    
    // Записать связь ПОСЛЕ телепортации
    // Записывает ТОЛЬКО одну связь (from → to)
    // Обратная связь запишется когда прыгнем обратно!
    // (есть исключения: X-1 → X-3 односторонний)
    public void discoverConnection(int fromMapId, int fromPortalId,
                                    int toMapId, int toPortalId) {
        // Записываем только фактически обнаруженную связь!
        connections.put(fromMapId + "-" + fromPortalId, 
            new PortalConnection(fromMapId, fromPortalId, toMapId, toPortalId));
        
        log.info("🔗 CONNECTION: Map {} Portal {} → Map {} Portal {}",
            fromMapId, fromPortalId, toMapId, toPortalId);
        
        // Обратную связь НЕ записываем автоматически!
        // Она запишется когда бот фактически прыгнет обратно.
        // Это важно, т.к. есть исключения (X-1 → X-3 односторонний)
    }
    
    // Построить маршрут (BFS)
    public List<PortalData> findPath(int fromMapId, int toMapId);
}
```

**Алгоритм записи связи:**

```
1. Перед телепортацией: запомнить fromMapId и fromPortalId
2. После телепортации: получаем MapInfoPacket новой карты
3. Найти портал на котором стоим (ближайший к позиции игрока)
4. Вызвать: discoverConnection(fromMapId, fromPortalId, toMapId, toPortalId)
5. Запишется ТОЛЬКО одна связь (from → to)
6. Обратная связь запишется когда бот фактически прыгнет обратно

Почему не записываем обратную автоматически:
- Есть исключение: X-1 → X-3 односторонний!
- Прыгнув с X-3 обратно, попадёшь на X-2, не на X-1!
- Поэтому записываем только фактически проверенные связи
```

**JSON формат (portals.json):**

```json
{
  "portalsByMap": {
    "1": [
      {
        "portalId": 123456,
        "mapId": 1,
        "x": 5000,
        "y": 3000,
        "type": 1,
        "targetMapId": 2,
        "targetPortalId": 789012,
        "useCount": 15
      }
    ]
  },
  "connections": {
    "1-123456": {
      "fromMapId": 1,
      "fromPortalId": 123456,
      "toMapId": 2,
      "toPortalId": 789012
    },
    "2-789012": {
      "fromMapId": 2,
      "fromPortalId": 789012,
      "toMapId": 1,
      "toPortalId": 123456
    }
  }
}
```

**ВАЖНО:** Связи записываются только при фактическом прыжке!
- Большинство порталов двусторонние → со временем появятся обе связи
- Исключение X-1 → X-3: обратной связи не будет (обратно с X-3 → X-2)

---

### NpcDatabase

Собирает статистику о типах NPC.

```java
public class NpcDatabase {
    private final Map<Integer, NpcTypeData> npcTypes;
    private final Map<Integer, Set<Integer>> npcsByMap;  // mapId → npcTypes
    
    public static class NpcTypeData {
        public int npcType;
        public int minHp, maxHp, avgHp;
        public float speed;
        public int estimatedDamage;
        public Set<Integer> seenOnMaps;
        public int killCount;
        public int sampleCount;
        public float lootDropRate;
    }
    
    // При появлении NPC
    public void discover(int npcType, int mapId, int hp, int maxHp, float speed);
    
    // При убийстве
    public void recordKill(int npcType, boolean droppedLoot);
    
    // При получении урона от NPC
    public void recordDamage(int npcType, int damage);
    
    // Получить NPC на карте
    public List<NpcTypeData> getNpcsOnMap(int mapId);
    
    // Оценить сложность
    public float estimateDifficulty(int npcType);
}
```

**JSON формат (npcs.json):**

```json
{
  "1": {
    "npcType": 1,
    "minHp": 800,
    "maxHp": 1000,
    "avgHp": 900,
    "speed": 200,
    "estimatedDamage": 50,
    "seenOnMaps": [1, 2],
    "killCount": 1523,
    "sampleCount": 2000,
    "lootDropRate": 0.92
  }
}
```

---

### SpawnPointTracker

Отслеживает где появляются сущности.

```java
public class SpawnPointTracker {
    private static final int GRID_SIZE = 500;
    
    public record SpawnKey(int mapId, int gridX, int gridY) {}
    
    public static class SpawnPointData {
        public int mapId;
        public float centerX, centerY;
        public Set<Integer> npcTypes;
        public Set<Integer> boxTypes;
        public int npcSpawnCount;
        public int boxSpawnCount;
    }
    
    public void recordNpcSpawn(int mapId, float x, float y, int npcType);
    public void recordBoxSpawn(int mapId, float x, float y, int boxType);
    
    // Лучшие точки для фарма
    public List<SpawnPointData> getBestFarmSpots(int mapId, int minNpcCount);
}
```

---

### DiscoveryCollector

Центральный компонент, интегрирующий всё.

```java
public class DiscoveryCollector {
    private final MapRegistry maps;
    private final PortalGraph portals;
    private final NpcDatabase npcs;
    private final SpawnPointTracker spawns;
    
    // Запоминаем откуда прыгали
    private int lastMapId = -1;
    private int lastPortalId = -1;
    
    public void onMapInfo(MapInfoPacket packet) {
        maps.discover(packet);
        portals.discoverPortals(packet.mapId, packet.teleports);
        
        // Проверяем была ли телепортация
        if (lastMapId != -1 && lastMapId != packet.mapId && lastPortalId != -1) {
            // Найти портал на котором мы стоим (ближайший)
            // После телепорта игрок всегда НА портале!
            int arrivedPortalId = findNearestPortalId(packet.teleports, playerX, playerY);
            
            // Записываем ДВУСТОРОННЮЮ связь!
            portals.discoverConnection(lastMapId, lastPortalId, 
                                        packet.mapId, arrivedPortalId);
        }
        
        lastMapId = packet.mapId;
        lastPortalId = -1;  // Сброс
    }
    
    // Вызывать ПЕРЕД прыжком в портал!
    public void onBeforeTeleport(int portalId) {
        lastPortalId = portalId;
    }
    
    public void onNpcSpawn(int mapId, int npcType, int hp, int maxHp, float speed) {
        npcs.discover(npcType, mapId, hp, maxHp, speed);
    }
    
    public void onNpcKilled(int npcType, boolean droppedLoot) {
        npcs.recordKill(npcType, droppedLoot);
    }
    
    private int findNearestPortalId(TeleportInfo[] teleports, float x, float y) {
        // После телепорта игрок стоит прямо на портале
        return Arrays.stream(teleports)
            .min(Comparator.comparingDouble(t -> distance(t.x, t.y, x, y)))
            .map(t -> t.id)
            .orElse(-1);
    }
}
```

---

## Использование в BotBrain

### Навигация

```java
void decideNavigating(WorldSnapshot world, List<Action> actions) {
    if (world.map().id() == targetMapId) {
        transitionTo(BotState.FARMING);
        return;
    }
    
    // Строим путь через известные порталы
    List<PortalData> path = discovery.portals().findPath(
        world.map().id(), targetMapId
    );
    
    if (path == null) {
        // Путь не найден — нужно исследовать
        transitionTo(BotState.EXPLORING);
        return;
    }
    
    PortalData nextPortal = path.get(0);
    float dist = world.player().distanceTo(nextPortal.x, nextPortal.y);
    
    if (dist > PORTAL_JUMP_DISTANCE) {
        actions.add(Action.move(nextPortal.x, nextPortal.y));
    } else {
        // Запоминаем откуда прыгаем для Discovery!
        discovery.collector().onBeforeTeleport(nextPortal.portalId);
        actions.add(Action.useTeleport(nextPortal.portalId));
    }
}
```

### Выбор карты для фарма

```java
int selectBestFarmMap(int targetNpcType) {
    List<Integer> maps = discovery.npcs().findMapsWithNpc(targetNpcType);
    
    // Выбираем карту с наибольшим количеством спавнов
    return maps.stream()
        .max(Comparator.comparingInt(mapId -> 
            discovery.spawns().getBestFarmSpots(mapId, 5).size()
        ))
        .orElse(-1);
}
```

### Оценка сложности NPC

```java
boolean canHandle(NpcEntity npc, PlayerState player) {
    float difficulty = discovery.npcs().estimateDifficulty(npc.npcType());
    float myPower = player.hp() + player.shield();
    
    return myPower > difficulty * 0.5f;
}
```

---

## Пример эволюции данных

```
День 1: Первый запуск
─────────────────────
data/maps.json: {}
data/portals.json: {}
data/npcs.json: {}

Бот заходит на X-1:
🗺️ NEW MAP: X-1 (id=1)
🚪 NEW PORTAL P1 at (5000, 3000)   → на X-2
🚪 NEW PORTAL P2 at (15000, 12000) → на X-3 (односторонний!)

Бот убивает Streuner:
👾 NEW NPC TYPE: 1 (avgHp=900)

Бот телепортируется через P1 на X-2:
🗺️ NEW MAP: X-2 (id=2)
🔗 CONNECTION: Map 1 Portal P1 → Map 2 Portal P3
   (записана только одна связь)

Бот прыгает обратно через P3 на X-1:
🔗 CONNECTION: Map 2 Portal P3 → Map 1 Portal P1
   (теперь есть обе связи — портал двусторонний!)

Бот прыгает через P2 на X-3:
🗺️ NEW MAP: X-3 (id=3)
🔗 CONNECTION: Map 1 Portal P2 → Map 3 Portal P4
   (ЭТО ОДНОСТОРОННИЙ портал!)

Бот прыгает "обратно" через P4... но попадает на X-2!
🗺️ MAP: X-2 (id=2)
🔗 CONNECTION: Map 3 Portal P4 → Map 2 Portal P5
   (НЕ на X-1! Это исключение X-1→X-3)

День 7: Много данных
────────────────────
maps.json: 15 карт
portals.json: 40+ порталов, ~70 связей
   (большинство двусторонние, X-1→X-3 односторонний)
npcs.json: 20+ типов NPC

Бот теперь может:
✅ Построить маршрут X-1 → X-5 через 3 портала
✅ Вернуться X-5 → X-1 (по известным связям)
✅ Знает что X-1→X-3 односторонний (обратная связь на X-2!)
✅ Выбрать карту с нужными мобами
✅ Оценить сложность NPC перед боем
```

---

## Структура файлов

```
data/
├── maps.json       # Информация о картах
├── portals.json    # Порталы и связи
├── npcs.json       # Типы NPC
└── spawns.json     # Точки спавна
```

Все файлы создаются автоматически при первом запуске.
