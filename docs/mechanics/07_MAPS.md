# 07. Maps & Navigation (Карты и навигация)

## Обзор

Вселенная WarUniverse состоит из карт, соединённых порталами:
- 3 фракции = 3 набора карт
- Карты от X-1 до X-7 для каждой фракции
- Нейтральные карты (T-1, G-1)
- Junction карты (J-VO, J-VS, J-SO)

---

## Структура карт

### Orion Empire (E)

```
E-1 (Base) ─── E-2 ─── E-3 ─── J-VO ─── E-5 ─── E-6 ─── E-7 (Base)
                              │
                              └─── J-SO
```

| Карта | Тип | NPC | Safe Zone |
|-------|-----|-----|-----------|
| E-1 | Base | Hydro, Jenta | ✅ База |
| E-2 | Farm | Jenta, Mali | ❌ |
| E-3 | Farm | Mali, Plarion | ❌ |
| J-VO | Junction | Mixed | ❌ PvP! |
| J-SO | Junction | Mixed | ❌ PvP! |
| E-5 | Battle | Bangoliour | ❌ |
| E-6 | Battle | Zavientos, Magmius | ❌ |
| E-7 | Base | Raider, Vortex | ✅ База |

### Vega Union (U)

```
U-1 (Base) ─── U-2 ─── U-3 ─── J-VS ─── U-5 ─── U-6 ─── U-7 (Base)
                              │
                              └─── J-VO
```

| Карта | Тип | NPC | Safe Zone |
|-------|-----|-----|-----------|
| U-1 | Base | Hydro, Jenta | ✅ База |
| U-2 | Farm | Jenta, Mali | ❌ |
| U-3 | Farm | Mali, Plarion | ❌ |
| J-VS | Junction | Mixed | ❌ PvP! |
| J-VO | Junction | Mixed | ❌ PvP! |
| U-5 | Battle | Bangoliour | ❌ |
| U-6 | Battle | Zavientos, Magmius | ❌ |
| U-7 | Base | Raider, Vortex | ✅ База |

### Solar Conglomerate (R)

```
R-1 (Base) ─── R-2 ─── R-3 ─── J-VS ─── R-5 ─── R-6 ─── R-7 (Base)
                              │
                              └─── J-SO
```

| Карта | Тип | NPC | Safe Zone |
|-------|-----|-----|-----------|
| R-1 | Base | Hydro, Jenta | ✅ База |
| R-2 | Farm | Jenta, Mali | ❌ |
| R-3 | Farm | Mali, Plarion | ❌ |
| J-VS | Junction | Mixed | ❌ PvP! |
| J-SO | Junction | Mixed | ❌ PvP! |
| R-5 | Battle | Bangoliour | ❌ |
| R-6 | Battle | Zavientos, Magmius | ❌ |
| R-7 | Base | Raider, Vortex | ✅ База |

### Нейтральные карты

| Карта | Описание | Доступ |
|-------|----------|--------|
| T-1 | PvP арена, Spaceball | Level 9+ |
| G-1 | Special events | Level ? |

---

## Junction карты

Соединяют территории фракций.

```
       [E-3]
         │
    J-VO ├──── J-VS
         │         │
       [U-3]     [R-3]
         │         │
    J-SO ─────────┘
```

| Junction | Соединяет | Опасность |
|----------|-----------|-----------|
| J-VO | E ↔ U | ⚠️ PvP зона |
| J-VS | U ↔ R | ⚠️ PvP зона |
| J-SO | R ↔ E | ⚠️ PvP зона |

---

## Порталы (Teleports)

### Типы порталов

```
type 1: Обычный портал (между картами)
type 2: Специальный портал (Star Missions, Events)
type 3: Временный портал (Events)
```

### Использование портала

```
1. Подлететь к порталу (distance < 100)
2. TeleportRequest(portalId)
3. Сервер отправляет MapInfoPacket новой карты
```

### Данные портала (TeleportInfo)

```java
public class TeleportInfo {
    public int id;        // Уникальный ID
    public float x, y;    // Координаты на карте
    public int type;      // Тип портала
    public int subtype;   // Подтип
}
```

---

## Размеры карт

```
Стандартный размер: ~20,000 × 15,000 единиц
Вариации: от 15,000 × 12,500 до 25,000 × 20,000
```

### MapInfoPacket

```java
public class MapInfoPacket {
    public int mapId;
    public String mapName;
    public int width, height;
    public boolean hasSpaceStation;
    public float spaceStationX, spaceStationY;
    public TeleportInfo[] teleports;
}
```

---

## Навигация для бота

### Discovery System

Бот автоматически собирает информацию о картах:

```java
// При входе на карту
void onMapInfo(MapInfoPacket packet) {
    mapRegistry.discover(packet);
    portalGraph.discoverPortals(packet.mapId, packet.teleports);
}

// После телепортации
void onTeleportComplete(int toMapId, float toX, float toY) {
    portalGraph.discoverConnection(lastMapId, lastX, lastY, toMapId, toX, toY);
}
```

### Построение маршрута (BFS)

```java
List<PortalData> findPath(int fromMapId, int toMapId) {
    Queue<Integer> queue = new LinkedList<>();
    Map<Integer, PortalData> parent = new HashMap<>();
    
    queue.add(fromMapId);
    
    while (!queue.isEmpty()) {
        int current = queue.poll();
        
        if (current == toMapId) {
            return reconstructPath(parent, fromMapId, toMapId);
        }
        
        for (PortalData portal : portalGraph.getPortals(current)) {
            if (portal.targetMapId != null && !parent.containsKey(portal.targetMapId)) {
                parent.put(portal.targetMapId, portal);
                queue.add(portal.targetMapId);
            }
        }
    }
    
    return null; // Путь не найден
}
```

---

## Доступ к картам по уровням

| Уровень | Доступные карты |
|---------|-----------------|
| 1 | X-1 |
| 3 | X-2 |
| 5 | X-3 |
| 7 | X-4 (Junction) |
| 9 | X-5, T-1 |
| 12 | X-6 |
| 15 | X-7 |
| 17 | Все карты |

---

## Safe Zones

### Где есть Safe Zone

- X-1 — вокруг базы
- X-7 — вокруг базы
- Некоторые Trading Station

### Эффект Safe Zone

```
В Safe Zone:
- Нельзя атаковать
- Нельзя быть атакованным
- Автоматический ремонт
- Можно покинуть в любой момент
```

### Определение Safe Zone

```java
// GameStateResponsePacket
boolean safeZone = packet.safeZone;

// Или по расстоянию до базы
boolean nearBase = distance(player, base) < SAFE_ZONE_RADIUS;
```

---

## Стратегия навигации для бота

### Безопасный фарм

```
1. Оставаться в своей фракции (X-1 до X-3)
2. Избегать Junction карт (PvP)
3. При опасности — отступать к X-1
```

### Агрессивный фарм

```
1. Фарм на X-5, X-6 для лучших наград
2. Готовность к побегу через порталы
3. Знание маршрута к ближайшей базе
```

### Навигация к цели

```java
void navigateTo(int targetMapId) {
    List<PortalData> path = findPath(currentMapId, targetMapId);
    
    if (path == null) {
        // Исследовать ближайшие порталы
        explore();
        return;
    }
    
    PortalData nextPortal = path.get(0);
    
    if (distance(player, nextPortal) > 100) {
        moveTo(nextPortal.x, nextPortal.y);
    } else {
        useTeleport(nextPortal.id);
    }
}
```

---

## Специальные карты

### T-1 (Tournament)

```
Доступ: Level 9+
NPC: Нет
Назначение: PvP арена, Spaceball
Порталы:
- Верх: R-5
- Право: E-5
- Лево: U-5
- Низ: R-3, E-3, U-3
```

### G-1 (Galaxy)

```
Доступ: Специальный
NPC: Event-specific
Назначение: Spaceball (расширенный)
Особенность: Случайное появление при телепортации
```

---

## Связанные механики

- [Factions](./08_FACTIONS.md) — территории фракций
- [Safe Zones](./16_SAFE_ZONES.md) — безопасные зоны
- [Leveling](./09_LEVELING.md) — доступ к картам
- [PvP](./17_PVP.md) — опасность Junction карт
