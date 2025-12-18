# WarUniverse Bot - Архитектура v2.1

## Философия

1. **Один поток — один игровой цикл** — никаких race conditions
2. **Параллельные действия** — атака и сбор работают одновременно
3. **Реактивность** — никаких таймаутов, только события
4. **Конечный автомат (FSM)** — предсказуемые переходы состояний
5. **Self-Learning** — бот сам собирает информацию о мире (Discovery System)

---

## Ключевые открытия из анализа PCAP

### Атака и сбор НЕ конфликтуют!

```
LOCK → ждём подтверждение (PARAM_TARGET_ID=20) → ATTACK
        ↓
Атака продолжается автоматически
MOVE куда угодно → атака НЕ прерывается
Моб агрится и СЛЕДУЕТ за игроком
Можно собирать боксы во время боя (радиус 600-900)
```

### Правильный протокол сбора

```
1. MOVE to (box.x, box.y + 97)    // Y_OFFSET = 97!
2. Wait for arrival (~700-900ms)  // ЖДАТЬ ОБЯЗАТЕЛЬНО!
3. CollectableCollectRequest       // ТОЛЬКО ПОСЛЕ прибытия
```

**ВАЖНО: MOVE и COLLECT отправляются ПОСЛЕДОВАТЕЛЬНО, не одновременно!**

Быстрый сбор БЕЗ движения и ожидания НЕ работает!

---

## Высокоуровневая архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                         GAME SERVER                              │
└─────────────────────────────┬───────────────────────────────────┘
                              │ TCP (KryoNet)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     1. NETWORK LAYER                             │
│  Connection.java │ PacketSender.java │ PacketRegistry.java       │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     2. PROTOCOL LAYER                            │
│  PacketProcessor.java + DiscoveryCollector                       │
└─────────────────────────────┬───────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ 3. WORLD STATE  │  │ 4. DISCOVERY DB │  │ 5. PERSISTENCE  │
│                 │  │                 │  │                 │
│ PlayerState     │  │ MapRegistry     │  │ maps.json       │
│ NPCs, Boxes     │  │ PortalGraph     │  │ portals.json    │
│ Players         │  │ NpcDatabase     │  │ npcs.json       │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      6. BRAIN (AI Layer)                         │
│                                                                  │
│  BotBrain (FSM) + CombatBrain + CollectBrain + SafetyBrain      │
│                                                                  │
│  States: IDLE → FARMING → FLEEING → REPAIRING → NAVIGATING      │
│                    ↓                                             │
│              SWITCHING_SHIP                                      │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     7. ACTION LAYER                              │
│  Action.java (sealed interface) │ ActionExecutor.java            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Игровой цикл (один поток!)

```java
while (running) {
    // 1. Получить пакеты
    List<Object> packets = connection.pollPackets();
    
    // 2. Обработать пакеты → обновить World
    for (Object packet : packets) {
        processor.process(packet, world);
    }
    
    // 3. AI принимает решение
    List<Action> actions = brain.decide(world.snapshot());
    
    // 4. Выполнить действия
    for (Action action : actions) {
        executor.execute(action);
    }
    
    // 5. Tick rate
    Thread.sleep(100);  // 10 Hz
}
```

---

## Состояния FSM (BotState)

| Состояние | Описание | Переход в |
|-----------|----------|-----------|
| IDLE | Ожидание | FARMING, NAVIGATING |
| FARMING | Фарм NPC + сбор | FLEEING, REPAIRING, SWITCHING_SHIP |
| COLLECTING | Только сбор | FARMING, FLEEING |
| FLEEING | Побег от врагов | REPAIRING, FARMING |
| REPAIRING | Ремонт на базе | FARMING |
| NAVIGATING | Перемещение между картами | FARMING, EXPLORING |
| SWITCHING_SHIP | Смена корабля (в любой safe zone) | FARMING |
| EXPLORING | Исследование | FARMING |
| STAR_MISSION | Galaxy Gates | FARMING |

---

## Discovery System

Бот автоматически записывает всё что видит:

| Данные | Источник | Когда |
|--------|----------|-------|
| Карты | MapInfoPacket | При входе |
| Порталы | MapInfoPacket.teleports | При входе |
| Связи порталов | После телепортации | Сравнение до/после |
| NPC типы | GameStateResponsePacket | При появлении |
| NPC характеристики | HP/Speed changes | Во время боя |
| Spawn points | Координаты сущностей | Накопление |

Данные сохраняются в JSON и используются для:
- Построения маршрутов через порталы
- Выбора карты с нужными мобами
- Оценки сложности NPC

---

## Смена кораблей (вместо продажи)

```java
if (player.isCargoFull()) {
    if (shipManager.hasAvailableShip()) {
        transitionTo(SWITCHING_SHIP);  // Меняем в любой Safe Zone
    } else {
        log.warn("All ships full!");   // Продолжаем фарм БЕЗ сбора лута
    }
}
```

**Где можно менять корабль:**
- В любой Safe Zone на союзной карте
- У любого портала (если safe zone активна)
- На базе фракции (X-7)
- На стартовых картах (X-1) — везде безопасно

Не нужно лететь на базу — достаточно быть в safe zone!

---

## Константы

```java
// Сбор
float Y_OFFSET = 97f;           // Смещение для сбора
float COLLECT_RADIUS = 800f;    // Радиус сбора во время боя

// Kite distances
float KITE_BOSS = 850f;         // HP >= 500k
float KITE_MEDIUM = 800f;       // HP >= 70k
float KITE_WEAK = 750f;         // HP < 70k

// Safety
float FLEE_HP_PERCENT = 0.30f;
float CRITICAL_HP_PERCENT = 0.15f;

// Tick rate
int TICK_INTERVAL_MS = 100;     // 10 Hz
int PACKET_MIN_INTERVAL_MS = 50;
```

---

## См. также

- [PACKETS.md](./PACKETS.md) — Протокол пакетов
- [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) — План реализации
- [DISCOVERY.md](./DISCOVERY.md) — Discovery System детали
