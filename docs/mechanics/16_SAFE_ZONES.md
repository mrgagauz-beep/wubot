# 16. Safe Zones & Repair (Безопасные зоны и ремонт)

## Обзор

Безопасные зоны — места где нельзя получить урон и нельзя атаковать.

---

## Типы безопасных зон

### 1. Карты X-1 (стартовые)

```
На картах X-1 ВЕЗДЕ безопасно!
- Враги (NPC и игроки) не атакуют пока мы их не тронем
- Можно спокойно летать по всей карте
```

### 2. Карты X-7 (базы)

```
На картах X-7:
- Безопасно в близости порталов (как на обычных картах)
- Безопасно в близости базы фракции
- Расположение базы приходит в пакетах (MapInfoPacket)
```

### 3. Порталы на союзных картах (X-2 до X-6)

```
Расположение порталов: приходит в MapInfoPacket.teleports
Безопасная зона активна при условиях (см. ниже)
```

### 4. Space Stations

```
Расположение: MapInfoPacket.hasSpaceStation, spaceStationX, spaceStationY
Безопасная зона вокруг станции
```

---

## ВАЖНО: Условия активации Safe Zone у порталов!

```
Safe Zone у портала срабатывает ТОЛЬКО если:
✅ Игрок НЕ получает урон в данный момент
✅ Игрок НЕ атакует в данный момент

Safe Zone у портала НЕ срабатывает если:
❌ Игрок под атакой (получает урон)
❌ Игрок атакует кого-то

Это критически важно для эвакуации!
```

---

## Фарм в безопасных зонах

```
МОЖНО фармить находясь у портала или в safe zone!
- Бить мобов
- Собирать лут
- В случае опасности — прыжок на другую карту

Это удобная стратегия:
- Фарм рядом с порталом
- Появился враг → прыжок → если союзная карта — безопасно
                          → если не союзная — сразу прыжок обратно
```

---

## Смена корабля в Safe Zone

```
Корабль можно менять в ЛЮБОЙ Safe Zone на союзной карте!

Где можно:
✅ У любого портала (если safe zone активна)
✅ На базе фракции (X-7)
✅ У Space Station
✅ На стартовых картах (X-1) — везде!

Не нужно лететь на базу — достаточно быть в safe zone!
```

### Стратегия для бота

```
1. Фармим рядом с порталом
2. Cargo заполнился → подлетаем к порталу
3. Меняем корабль прямо здесь!
4. Продолжаем фармить

Не нужно лететь на X-1 или X-7!
```

---

## Алгоритм эвакуации

### КРИТИЧЕСКИ ВАЖНО: Не останавливаться!

```
Телепорт происходит НЕ мгновенно!
Если остановиться у портала — можно умереть пока ждёшь телепорт.

ПРАВИЛЬНО:
- Лететь К порталу
- ПРОЛЕТЕТЬ ЧЕРЕЗ портал без остановки
- Отправить TeleportRequest когда рядом с порталом
- Продолжать лететь в том же направлении
- Телепорт произойдёт "на лету"
```

### Визуализация правильной эвакуации

```
❌ НЕПРАВИЛЬНО (остановка):

    [Enemy]→→→[BOT]════════►[Portal]
                              ↓
                           [STOP]  ← Ждём телепорт
                              ↓
                           [DEAD]  ← Враг догнал и убил


✅ ПРАВИЛЬНО (пролёт без остановки):

    [Enemy]→→→[BOT]════════►[Portal]════════►
                           │         │
                    TeleportRequest  (продолжаем лететь!)
                           │         ↓
                           └────(телепорт на лету)────┘
                                     ↓
    ════════════════►[BOT]════════════════►[Safe Zone]
    (продолжаем лететь на новой карте)

Последовательность:
1. MOVE к точке ЗА порталом (не к порталу!)
2. Когда рядом с порталом → отправить TeleportRequest
3. ПРОДОЛЖАТЬ лететь! Не останавливаться!
4. Телепорт произойдёт пока летим
5. На новой карте — появляемся НА портале
```

### Порталы двусторонние!

```
ВАЖНО понимать:

Карта A                      Карта B
┌──────────────┐              ┌──────────────┐
│              │              │              │
│   [Portal]═══┼══════════════┼══[Portal]   │
│      ↑       │              │      ↑       │
│    [BOT]     │              │    [BOT]     │
│              │              │              │
└──────────────┘              └──────────────┘

1. Прыгнули в портал на карте A
2. Появились НА портале карты B (мы УЖЕ на портале!)
3. Если B союзная → сразу safe zone → ремонт
4. Если B вражеская → сразу TeleportRequest → обратно на A
5. Никуда лететь не надо — мы уже на портале!
```

### Код эвакуации

```java
class EvacuationBrain {
    Portal targetPortal;
    Vector2 fleeTarget;
    boolean teleportRequested = false;
    
    void startEvacuation(List<Enemy> enemies) {
        // 1. Переключить на Config 2 (макс скорость)
        switchConfig(CONFIG_ESCAPE);
        
        // 2. Найти безопасный портал
        targetPortal = findEvacuationPortal(enemies);
        
        // 3. Вычислить точку ЗА порталом (не К порталу!)
        Vector2 direction = normalize(targetPortal.pos - player.pos);
        fleeTarget = targetPortal.pos + direction * 1000;
        
        teleportRequested = false;
    }
    
    // Вызывается каждый tick!
    void update() {
        // 4. ВСЕГДА лететь к точке ЗА порталом
        moveTo(fleeTarget);  // Не останавливаемся!
        
        // 5. Когда рядом с порталом — отправить TeleportRequest
        if (!teleportRequested && distanceTo(targetPortal) < PORTAL_JUMP_DISTANCE) {
            send(new TeleportRequest(targetPortal.id));
            teleportRequested = true;
            // ПРОДОЛЖАЕМ лететь! Не ждём! Не останавливаемся!
        }
    }
}

void onMapChanged(MapInfoPacket newMap) {
    if (isEvacuating) {
        // Телепорт произошёл — мы на новой карте
        // Игрок появляется НА ПОРТАЛЕ!
        
        if (isAlliedMap(newMap)) {
            // Союзная карта — на портале сразу safe zone!
            isEvacuating = false;
            startRepairIfNeeded();
        } else {
            // НЕ союзная карта — прыгаем ОБРАТНО!
            // Порталы ДВУСТОРОННИЕ — мы уже НА портале!
            // Просто отправить TeleportRequest снова — без движения!
            
            Portal currentPortal = findPortalAtPlayerPosition();
            send(new TeleportRequest(currentPortal.id));
            // Прыгаем обратно на предыдущую карту (союзную)
        }
    }
}
```

### Выбор портала для эвакуации

```java
Portal findEvacuationPortal(List<Enemy> enemies) {
    // Получить порталы из MapInfoPacket.teleports
    List<Portal> portals = currentMap.teleports;
    
    // Фильтр: только НЕ-PvP порталы!
    portals = portals.stream()
        .filter(p -> !isPvPDestination(p))
        .collect(toList());
    
    // Сортировка по расстоянию
    portals.sort(byDistanceFrom(player.pos));
    
    Portal nearest = portals.get(0);
    
    // Проверить: враги на пути к ближайшему?
    if (enemiesBlockPath(enemies, nearest)) {
        // Выбрать ближайший порт В ДРУГОЙ СТОРОНЕ от врагов
        return findPortalAwayFromEnemies(portals, enemies);
    }
    
    return nearest;
}

boolean enemiesBlockPath(List<Enemy> enemies, Portal target) {
    Vector2 pathDirection = normalize(target.pos - player.pos);
    
    for (Enemy enemy : enemies) {
        Vector2 toEnemy = normalize(enemy.pos - player.pos);
        float dot = dot(pathDirection, toEnemy);
        
        // Враг впереди по направлению к порталу?
        if (dot > 0.5f && distanceTo(enemy) < distanceTo(target)) {
            return true;
        }
    }
    return false;
}

Portal findPortalAwayFromEnemies(List<Portal> portals, List<Enemy> enemies) {
    // Средняя позиция врагов
    Vector2 enemyCenter = averagePosition(enemies);
    
    // Направление ОТ врагов
    Vector2 awayDirection = normalize(player.pos - enemyCenter);
    
    // Найти портал в этом направлении
    return portals.stream()
        .filter(p -> !isPvPDestination(p))
        .max((a, b) -> {
            float scoreA = dot(awayDirection, normalize(a.pos - player.pos));
            float scoreB = dot(awayDirection, normalize(b.pos - player.pos));
            return Float.compare(scoreA, scoreB);
        })
        .orElse(portals.get(0));
}
```

---

## PvP карты — ИЗБЕГАТЬ при эвакуации!

### Список PvP карт

```
Junction карты (между фракциями):
- J-VO (Vega ↔ Orion)
- J-VS (Vega ↔ Solar)  
- J-SO (Solar ↔ Orion)

Специальные:
- T-1 (Spaceball арена)
- G-1 (Ивент карта)

На этих картах:
- НЕТ безопасных зон
- ВСЕ игроки могут атаковать друг друга
- Порталы НЕ дают защиту
```

### Фильтр порталов

```java
boolean isPvPDestination(Portal portal) {
    String destMap = getDestinationMap(portal);
    
    // PvP карты
    if (destMap.startsWith("J-")) return true;  // Junction
    if (destMap.equals("T-1")) return true;     // Spaceball
    if (destMap.equals("G-1")) return true;     // Event
    
    return false;
}
```

---

## Ремонт

### Где ремонтировать

```
1. Карта X-1 — везде безопасно
2. База фракции (X-7) — координаты из MapInfoPacket
3. Space Station — если есть на карте
4. Портал на союзной карте — если не под атакой
```

### Стоимость ремонта

```
С Premium: БЕСПЛАТНО
Без Premium: 500 PLT

Исключение:
- PvP смерть: всегда 500 PLT (даже с Premium)
```

### Автоматический ремонт

```
В safe zone:
- HP восстанавливается автоматически
- Shield восстанавливается автоматически
```

---

## Определение Safe Zone

### Из пакетов

```java
// GameStateResponsePacket
boolean safeZone;  // Текущий статус от сервера

// MapInfoPacket
boolean hasSpaceStation;
float spaceStationX, spaceStationY;
TeleportInfo[] teleports;  // Позиции порталов
```

### Проверка

```java
boolean isCurrentlySafe() {
    // Сервер сообщает статус safe zone
    return gameState.safeZone;
}
```

---

## Триггеры для эвакуации

```java
boolean shouldEvacuate() {
    // 1. Критическое HP
    if (player.hpPercent < CRITICAL_HP_PERCENT) {
        return true;
    }
    
    // 2. Вражеские игроки рядом
    if (hasEnemyPlayersNearby()) {
        return true;
    }
    
    // 3. Слишком много агрессивных NPC
    if (aggroedNpcCount > MAX_AGGRO_COUNT) {
        return true;
    }
    
    return false;
}
```

---

## Полный FSM эвакуации

```
┌─────────────────────────────────────────────────────────────┐
│                    EVACUATION FSM                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  FARMING ──[угроза]──→ EVALUATING                           │
│                            │                                │
│                ┌───────────┴───────────┐                    │
│                ▼                       ▼                    │
│         [враги блокируют     [путь свободен]                │
│          ближайший порт]           │                        │
│                │                   │                        │
│                ▼                   ▼                        │
│         FLEE_AWAY          FLEE_TO_NEAREST                  │
│         (противоположный)  (ближайший порт)                 │
│                │                   │                        │
│                └─────────┬─────────┘                        │
│                          ▼                                  │
│               FLYING_THROUGH_PORTAL                         │
│               (НЕ останавливаться!)                         │
│               + TeleportRequest                             │
│                          │                                  │
│               ┌──────────┴──────────┐                       │
│               ▼                     ▼                       │
│        [союзная карта]       [вражеская/pvp]                │
│               │                     │                       │
│               ▼                     ▼                       │
│            SAFE!            INSTANT_JUMP_BACK               │
│        (на портале)         (мы уже НА портале!             │
│               │              сразу TeleportRequest          │
│               │              никуда лететь не надо!)        │
│               │                     │                       │
│               ▼                     │                       │
│           REPAIRING ←───────────────┘                       │
│               │                                             │
│               ▼                                             │
│           FARMING                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Константы

```java
// Эвакуация
float PORTAL_JUMP_DISTANCE = 100f;      // Когда отправлять TeleportRequest
float FLEE_BEYOND_PORTAL = 1000f;       // Лететь ЗА портал, не К нему
float ENEMY_DETECTION_RANGE = 2000f;
float PATH_BLOCK_ANGLE = 0.5f;          // cos(60°)

// Ремонт
float FLEE_HP_PERCENT = 0.30f;
float CRITICAL_HP_PERCENT = 0.15f;
```

---

## Связанные механики

- [Maps](./07_MAPS.md) — карты и порталы
- [Factions](./08_FACTIONS.md) — союзные/вражеские карты
- [Combat](./01_COMBAT.md) — детекция угроз
- [Ships](./02_SHIPS.md) — Config 2 для побега
