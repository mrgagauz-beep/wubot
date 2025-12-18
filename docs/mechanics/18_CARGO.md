# 18. Cargo & Collection (Карго и сбор)

## Обзор

Система сбора ресурсов в WarUniverse:
- **Cargo** — грузовой отсек корабля
- **Collectables** — боксы для сбора (лут с NPC)
- **Collection** — процесс подбора боксов

---

## Cargo (Грузовой отсек)

### Размеры по кораблям

| Корабль | Cargo |
|---------|-------|
| Shuttle | 100 |
| Zephyrus | 250 |
| Thorus | 500 |
| Veles | 750 |
| Calipso | 1,000 |
| Hecate | 2,000 |
| Svarog | 2,000 |
| Caviar | 2,500 |
| Hyperion | 3,000 |
| Perun | 3,000 |

### Что занимает Cargo

```
Ресурсы:
- Cerium, Mercury, Erbium, Piritid
- Darkonit, Uranit, Azurit
- Dungid, Xureon

НЕ занимает место:
- BTC
- PLT
- EXP, Honor
- Боеприпасы
- Параметры Star Missions
```

### Параметры в пакетах

```java
// GameStateResponsePacket → Change
PARAM_CARGO_USED = 32;  // Использовано места
PARAM_CARGO_MAX = 33;   // Максимум места

// Проверка
boolean isFull = cargoUsed >= cargoMax;
boolean almostFull = cargoUsed >= cargoMax * 0.9f;
```

---

## Collectables (Боксы)

### Типы боксов

| Type ID | Название | Содержимое | Собирать? |
|---------|----------|------------|-----------|
| 1 | BONUS_BOX | Амуниция, BTC | ✅ Всегда |
| 2 | RESOURCE_1 | Первичные ресурсы | ❌ Обычно игнор |
| 3 | RESOURCE_2 | Первичные ресурсы | ❌ Обычно игнор |
| 4 | LOOT | Ресурсы с NPC | ✅ Если есть место |
| 13 | SPECIAL_1 | Особое | ❌ Игнор |
| 14 | SPECIAL_2 | Особое | ❌ Игнор |

### Приоритет сбора

```
1. BONUS_BOX (type=1) — всегда, не занимает cargo
2. LOOT (type=4) — если cargo не полный
3. RESOURCE — обычно игнорировать
```

### Появление боксов

```
После убийства NPC:
- 1-3 бокса (зависит от типа NPC)
- Bonus Box + Loot (обычно)
- Время жизни: ~60-120 секунд

CollectableInPacket:
- id: уникальный ID бокса
- type: тип бокса
- x, y: координаты
- existOnMap: true (появился) / false (исчез)
```

---

## Протокол сбора

### Критическое открытие: Y_OFFSET = 97!

```
НЕ работает:
1. Прилететь к боксу (box.x, box.y)
2. CollectableCollectRequest
❌ Бокс не собирается!

ПРАВИЛЬНО работает:
1. MOVE к (box.x, box.y + 97)    // ВАЖНО: +97 к Y!
2. Ждать прибытия (~700-900ms)
3. CollectableCollectRequest(boxId)  // ТОЛЬКО ПОСЛЕ прибытия!
✅ Бокс собран!
```

### ВАЖНО: MOVE и COLLECT отправляются ПОСЛЕДОВАТЕЛЬНО!

```
❌ НЕПРАВИЛЬНО:
send(MOVE(box.x, box.y + 97));
send(CollectableCollectRequest(box.id));  // Сразу — НЕ РАБОТАЕТ!

✅ ПРАВИЛЬНО:
send(MOVE(box.x, box.y + 97));
waitForArrival();                         // ЖДАТЬ прибытия!
send(CollectableCollectRequest(box.id));  // Только после прилёта
```

### Правильная последовательность сбора

```
1. MOVE к позиции (box.x, box.y + 97) — смещение +97 по Y
2. ЖДАТЬ пока корабль долетит (время = расстояние / скорость)
3. CollectableCollectRequest с id = box.id (после прибытия!)
4. Сервер отвечает CollectableCollectedPacket с наградой
```

### Код сбора

```java
void collectBox(Box box) {
    // 1. Вычислить точку сбора
    float collectX = box.x;
    float collectY = box.y + Y_OFFSET;  // Y_OFFSET = 97
    
    // 2. Двигаться к точке сбора
    send(new MovePacket(collectX, collectY));
    
    // 3. ЖДАТЬ прибытия (ОБЯЗАТЕЛЬНО!)
    float distance = distanceTo(collectX, collectY);
    int waitTime = (int)(distance / playerSpeed * 1000) + 100;  // + запас
    waitForArrival(waitTime);  // ~700-900ms обычно
    
    // 4. Только теперь отправить запрос на сбор
    send(new CollectableCollectRequest(box.id));
}
```

### Параллельный сбор во время боя

```
Важное открытие из PCAP:
- LOCK + ATTACK → атака продолжается автоматически
- MOVE куда угодно → атака НЕ прерывается
- Моб агрится и СЛЕДУЕТ за игроком
- Можно собирать боксы в радиусе 600-900

Стратегия:
1. Начать атаку на NPC
2. Во время боя: собирать ближайшие боксы (НО с ожиданием!)
3. Возвращаться к орбите вокруг NPC
4. NPC умер → собрать его лут
```

### Алгоритм параллельного сбора

```java
void farmingLoop() {
    NPC target = selectTarget();
    lockAndAttack(target);
    
    while (target.isAlive()) {
        // Проверить боксы в радиусе сбора
        Box nearbyBox = findNearbyBox(COLLECT_RADIUS);  // 600-800
        
        if (nearbyBox != null && canCollect(nearbyBox)) {
            // Сбор с ОЖИДАНИЕМ!
            // Атака продолжается автоматически!
            
            // 1. MOVE к боксу
            float collectX = nearbyBox.x;
            float collectY = nearbyBox.y + Y_OFFSET;
            send(new MovePacket(collectX, collectY));
            
            // 2. ЖДАТЬ прибытия
            float distance = distanceTo(collectX, collectY);
            int waitTime = (int)(distance / playerSpeed * 1000) + 100;
            sleep(waitTime);
            
            // 3. Только после прилёта — собрать
            send(new CollectableCollectRequest(nearbyBox.id));
            
        } else {
            // Орбита вокруг NPC (обычный kiting)
            orbitStep(target);
        }
        
        sleep(200);
    }
    
    // NPC мёртв — собрать его лут (тоже с ожиданием!)
    collectDroppedLoot(target.getPosition());
}

void collectDroppedLoot(Position npcPos) {
    List<Box> lootBoxes = findBoxesNear(npcPos, 200);
    
    for (Box box : lootBoxes) {
        // Каждый бокс: MOVE → ЖДАТЬ → COLLECT
        float collectX = box.x;
        float collectY = box.y + Y_OFFSET;
        
        send(new MovePacket(collectX, collectY));
        
        float distance = distanceTo(collectX, collectY);
        int waitTime = (int)(distance / playerSpeed * 1000) + 100;
        sleep(waitTime);  // ОБЯЗАТЕЛЬНО ждать!
        
        send(new CollectableCollectRequest(box.id));
    }
}
```

---

## Константы сбора

```java
// Смещение для сбора
float Y_OFFSET = 97f;

// Радиус сбора во время боя
float COLLECT_RADIUS_COMBAT = 800f;

// Радиус сбора вне боя
float COLLECT_RADIUS_IDLE = 1500f;

// Время ожидания после MOVE
int COLLECT_WAIT_MS = 800;

// Минимальное расстояние для сбора
float MIN_COLLECT_DISTANCE = 50f;
```

---

## Действия при полном Cargo

### Вариант 1: Возврат на базу

```java
void onCargoFull() {
    // Прекратить фарм
    stopFarming();
    
    // Лететь на базу
    navigateTo(BASE_MAP);
    
    // Продать ресурсы
    sellCargo();
    
    // Вернуться к фарму
    navigateTo(FARM_MAP);
}
```

### Вариант 2: Смена корабля

```java
void onCargoFull() {
    if (hasAvailableShip()) {
        // Лететь на базу
        navigateTo(BASE_MAP);
        
        // Сменить корабль
        switchToNextShip();
        
        // Вернуться к фарму
        navigateTo(FARM_MAP);
    } else {
        // Все корабли заполнены — продавать
        sellAllCargos();
    }
}
```

### Вариант 3: Игнорировать (только Bonus Box)

```java
void collectBox(Box box) {
    if (box.type == BONUS_BOX) {
        // Bonus Box не занимает места
        collect(box);
    } else if (!isCargoFull()) {
        collect(box);
    }
    // Иначе игнорировать
}
```

---

## Управление Cargo

### Продажа ресурсов

```
Место: База фракции (X-1 или X-7)
Меню: Control Panel → Hangar → Resources → Sell
```

### Переработка (Refine)

```
Место: База или Premium (Autorefine)
Меню: Control Panel → Equipment → Resources → Refine
```

### Загрузка в улучшения

```
Меню: Control Panel → Equipment → Upgrades
Действие: Выбрать оборудование → добавить ресурсы
```

---

## Связанные механики

- [Resources](./06_RESOURCES.md) — типы ресурсов
- [Combat](./01_COMBAT.md) — параллельный сбор
- [Ships](./02_SHIPS.md) — размер cargo
- [Economy](./13_ECONOMY.md) — продажа
- [Safe Zones](./16_SAFE_ZONES.md) — где продавать
