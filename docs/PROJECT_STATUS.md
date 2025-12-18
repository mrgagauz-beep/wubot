# WuBot - Project Status

## Текущий статус

**Дата**: 2025-12-16
**Версия**: MVP (в разработке)

### Что реализовано

#### Network Layer (`com.wubot.network`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `Connection.java` | KryoNet клиент, подключение к серверу, очередь входящих пакетов | Работает |
| `PacketSender.java` | Отправка пакетов с rate limiting (50ms) | Работает |
| `PacketRegistry.java` | Регистрация всех Kryo классов для сериализации | Работает |

#### Protocol Layer (`com.wubot.protocol`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `PacketProcessor.java` | Обработка входящих пакетов, обновление World | Требует отладки |
| `ParamId.java` | Константы ID параметров (HP, POSITION, NPC_TYPE и т.д.) | Работает |
| `BoxType.java` | Типы боксов и логика фильтрации | Работает |
| `packets/*` | Классы пакетов (декомпилированы из клиента) | Работает |

#### World State (`com.wubot.world`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `World.java` | Главный контейнер состояния мира (thread-safe ConcurrentHashMap) | Работает |
| `WorldSnapshot.java` | Immutable снимок мира для принятия решений | Работает |
| `PlayerState.java` | Состояние своего корабля (HP, позиция, cargo) | Работает |
| `NpcEntity.java` | Состояние NPC (позиция, HP, тип, предсказание движения) | Работает |
| `PlayerEntity.java` | Состояние других игроков | Работает |
| `BoxEntity.java` | Состояние боксов (тип, позиция) | Работает |
| `MapInfo.java` | Информация о карте (размеры, safe zone) | Работает |

#### Brain / AI (`com.wubot.brain`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `BotBrain.java` | Главный FSM, координация всех подсистем | Требует отладки |
| `BotState.java` | Enum состояний (IDLE, FARMING, FLEEING и т.д.) | Работает |
| `BotConfig.java` | Конфигурация (HP пороги, дистанции кайта) | Работает |
| `CombatBrain.java` | Выбор цели, атака, кайтинг | Требует отладки |
| `CollectBrain.java` | Сбор боксов (MOVE → WAIT → COLLECT) | Требует отладки |
| `SafetyBrain.java` | Определение необходимости побега | Работает |
| `NavigationBrain.java` | Эвакуация, работа с порталами | Работает |

#### Action Layer (`com.wubot.action`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `Action.java` | Sealed interface всех действий (Move, Lock, Attack, Collect...) | Работает |
| `ActionExecutor.java` | Выполнение действий через PacketSender | Работает |

#### Discovery System (`com.wubot.discovery`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `DiscoveryCollector.java` | Сбор данных о картах, NPC, порталах | Работает |
| `PersistenceManager.java` | Сохранение/загрузка JSON | Работает |
| `MapRegistry.java` | Реестр карт | Работает |
| `PortalGraph.java` | Граф порталов с BFS | Работает |
| `NpcDatabase.java` | База данных NPC | Работает |

#### Ship Management (`com.wubot.ship`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `ShipInfo.java` | Информация о корабле | Работает |
| `ShipManager.java` | Управление кораблями, переключение | Работает |

#### Main (`com.wubot`)
| Класс | Назначение | Статус |
|-------|------------|--------|
| `WuBotApplication.java` | Точка входа, инициализация компонентов | Работает |
| `GameLoop.java` | Главный цикл (10 Hz) | Работает |

---

## Что работает

1. **Подключение к серверу** - авторизация по SID работает
2. **Получение пакетов** - GameStateResponsePacket, MapInfoPacket приходят
3. **Базовая FSM логика** - переходы между состояниями работают
4. **Безопасность** - детекция низкого HP и вражеских игроков
5. **Логирование** - все операции логируются

---

## Что НЕ работает

### 1. Фарм (бот не атакует мобов)

**Симптомы:**
- Бот находится в состоянии FARMING
- Лог показывает "No targets found"
- NPC не выбираются для атаки

**Гипотезы:**
1. `World.npcs` пустая коллекция — NPC не добавляются
2. `GameStateResponsePacket.ships[]` не содержит NPC
3. `extractNpcType()` возвращает 0 для всех кораблей
4. `WorldSnapshot.getNpcs()` возвращает пустой список

### 2. Сбор боксов (collect не работает)

**Симптомы:**
- Боксы видны в логах (если есть)
- `CollectBrain.findBestBox()` возвращает null
- Действие COLLECT не отправляется

**Гипотезы:**
1. `World.boxes` пустая коллекция
2. `BoxType.isCollectable()` возвращает false для всех типов
3. Боксы вне `COLLECT_RADIUS` (800 units)
4. `cargo_full` блокирует сбор

### 3. Патруль (бот стоит на месте)

**Симптомы:**
- После убийства моба бот не ищет следующего
- Нет движения между точками спавна

**Гипотезы:**
1. Патруль не реализован (состояние EXPLORING - stub)
2. `CombatBrain.selectTarget()` не находит NPC вне зоны видимости

---

## Известные проблемы

### КРИТИЧНЫЕ

1. **Бот не видит мобов**
   - **Файл**: `PacketProcessor.java:100-106`
   - **Проблема**: Логика `extractNpcType()` может не находить NPC_TYPE в changes
   - **Возможные причины**:
     - ParamId.NPC_TYPE (9) не соответствует реальному ID в протоколе
     - changes[] пустой или null для NPC
     - Тип данных в change.data не соответствует ожиданиям

2. **World.npcs не заполняется**
   - **Файл**: `PacketProcessor.java:143-158`
   - **Проблема**: Метод `processNpc()` не вызывается
   - **Диагностика**: Добавить лог в `processShip()` перед проверкой npcType

### СРЕДНИЕ

3. **Боксы могут не добавляться**
   - **Файл**: `PacketProcessor.java:307-320`
   - **Проблема**: `processCollectable()` может не вызываться
   - **Диагностика**: Проверить приходит ли CollectableInPacket

4. **playerId может быть неинициализирован**
   - **Файл**: `PacketProcessor.java:61-63`
   - **Проблема**: Если playerId = 0, свой корабль обрабатывается как другой игрок
   - **Диагностика**: Проверить что playerId > 0 после первого пакета

### НИЗКИЕ

5. **Патруль не реализован**
   - **Файл**: `BotBrain.java:212-215`
   - **Проблема**: EXPLORING состояние - пустой stub
   - **Решение**: Реализовать логику патрулирования

---

## Поток данных

```
┌─────────────────────────────────────────────────────────────────┐
│                          СЕРВЕР                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ GameStateResponsePacket
┌─────────────────────────────────────────────────────────────────┐
│  Connection.java                                                 │
│  - KryoNet Client                                               │
│  - incomingQueue.add(packet)                                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ pollPackets()
┌─────────────────────────────────────────────────────────────────┐
│  GameLoop.java:104-116                                          │
│  - for (packet : packets) processor.process(packet, world)      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ process()
┌─────────────────────────────────────────────────────────────────┐
│  PacketProcessor.java                                           │
│                                                                  │
│  processGameState() [line 59-90]                                │
│    ├── packet.ships[] ──► processShip() [line 95-126]          │
│    │     ├── extractNpcType() [line 131-138]                   │
│    │     │     └── ищет ParamId.NPC_TYPE (9) в changes[]       │
│    │     │                                                      │
│    │     ├── если npcType > 0:                                 │
│    │     │     └── processNpc() [line 143-158]                 │
│    │     │           └── world.getOrCreateNpc(ship.id)         │  ◄── КРИТИЧЕСКАЯ ТОЧКА #1
│    │     │                                                      │
│    │     ├── если ship.id != playerId:                         │
│    │     │     └── processOtherPlayer()                        │
│    │     │                                                      │
│    │     └── иначе:                                            │
│    │           └── processOwnShip()                            │
│    │                                                            │
│    └── packet.collectables[] ──► processCollectable()          │  ◄── КРИТИЧЕСКАЯ ТОЧКА #2
│                                    └── world.getOrCreateBox()   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│  World.java                                                      │
│  - ConcurrentHashMap<Integer, NpcEntity> npcs                   │
│  - ConcurrentHashMap<Integer, BoxEntity> boxes                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ world.snapshot()
┌─────────────────────────────────────────────────────────────────┐
│  WorldSnapshot.java [line 31-59]                                │
│  - npcs = Collections.unmodifiableList(world.getAllNpcs())      │  ◄── КРИТИЧЕСКАЯ ТОЧКА #3
│  - boxes = Collections.unmodifiableList(world.getAllBoxes())    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ brain.decide(snapshot)
┌─────────────────────────────────────────────────────────────────┐
│  BotBrain.java                                                   │
│                                                                  │
│  decideFarming() [line 88-122]                                  │
│    ├── combatBrain.getActions(world)                            │
│    │     └── CombatBrain.selectTarget() [line 87-102]          │  ◄── КРИТИЧЕСКАЯ ТОЧКА #4
│    │           └── for (npc : world.getNpcs()) ...             │
│    │                 └── если нет NPC ──► возвращает null      │
│    │                                                            │
│    └── collectBrain.getActions(world)                           │
│          └── CollectBrain.findBestBox() [line 89-118]          │  ◄── КРИТИЧЕСКАЯ ТОЧКА #5
│                └── for (box : world.getBoxes()) ...            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼ List<Action>
┌─────────────────────────────────────────────────────────────────┐
│  ActionExecutor.java                                             │
│  - execute(Action.Lock) ──► sender.lock()                       │
│  - execute(Action.Attack) ──► sender.attack()                   │
│  - execute(Action.Move) ──► sender.move()                       │
│  - execute(Action.Collect) ──► sender.collect()                 │
└─────────────────────────────────────────────────────────────────┘
```

### Где может теряться информация о мобах

| Точка | Файл:строка | Проблема | Как проверить |
|-------|-------------|----------|---------------|
| #1 | `PacketProcessor:101-103` | `extractNpcType()` возвращает 0 | Добавить лог `ship.changes[]` |
| #2 | `PacketProcessor:143` | `processNpc()` не вызывается | Лог в начале метода |
| #3 | `WorldSnapshot:53` | `world.getAllNpcs()` пустой | Лог размера коллекции |
| #4 | `CombatBrain:91` | `world.getNpcs()` пустой | Лог в начале цикла |
| #5 | `CollectBrain:93` | `world.getBoxes()` пустой | Лог в начале цикла |

### Где может теряться информация о боксах

| Точка | Файл:строка | Проблема | Как проверить |
|-------|-------------|----------|---------------|
| #1 | `PacketProcessor:81-88` | `packet.collectables` null или пустой | Лог перед циклом |
| #2 | `PacketProcessor:307-320` | `processCollectable()` не вызывается | Лог в начале метода |
| #3 | `CollectBrain:95-97` | `BoxType.isCollectable()` возвращает false | Лог типа бокса |

---

## Рекомендации для следующего чата

1. **Начать с отладки PacketProcessor** — это корень всех проблем
2. **Добавить DEBUG логирование** в критические точки (см. DEBUG_CHECKLIST.md)
3. **Проверить ParamId.NPC_TYPE** — значение 9 может быть неверным
4. **Проверить формат данных** — int[] vs Object для позиции
5. **Запустить бота с -Dlogback.configurationFile=debug** для подробных логов
