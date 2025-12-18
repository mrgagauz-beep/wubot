# WarUniverse Bot - План реализации

## Обзор фаз

| Фаза | Описание | Срок | Статус |
|------|----------|------|--------|
| 0 | Подготовка проекта | 1 день | ✅ |
| 1 | Network Layer | 2-3 дня | ✅ |
| 2 | World State | 2 дня | ✅ |
| 3 | Protocol Layer | 2 дня | ✅ |
| 4 | Action Layer | 1 день | ✅ |
| 5 | Brain (FSM) | 3-4 дня | ✅ |
| 6 | Game Loop | 1 день | ✅ |
| 7 | Discovery System | 3-4 дня | ✅ |
| 8 | Ship Management | 2 дня | ✅ |

---

## Фаза 0: Подготовка (1 день)

### Задачи
- [x] Создать Gradle проект
- [x] Настроить зависимости (KryoNet, Gson, SLF4J)
- [x] Скопировать пакеты из client-kryonet.jar
- [x] Создать базовую структуру директорий

### build.gradle

```groovy
plugins {
    id 'java'
    id 'application'
}

group = 'com.wubot'
version = '1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.esotericsoftware:kryonet:2.22.0-RC1'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'ch.qos.logback:logback-classic:1.4.11'
    
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

application {
    mainClass = 'com.wubot.WuBotApplication'
}
```

### Структура проекта

```
src/main/java/com/wubot/
├── WuBotApplication.java
├── GameLoop.java
├── network/
├── protocol/
├── world/
├── discovery/
├── persistence/
├── brain/
├── ship/
├── action/
├── config/
└── util/

src/main/resources/
└── logback.xml

data/
├── maps.json
├── portals.json
├── npcs.json
└── spawns.json
```

---

## Фаза 1: Network Layer (2-3 дня)

### Задачи
- [x] Connection.java — KryoNet клиент
- [x] PacketSender.java — отправка с rate limiting
- [x] PacketRegistry.java — регистрация классов Kryo
- [ ] Тест подключения и авторизации

### Connection.java (ключевые методы)

```java
public class Connection {
    private Client client;
    private ConcurrentLinkedQueue<Object> incomingQueue;
    
    public void connect(String host, int port);
    public void disconnect();
    public boolean isConnected();
    public List<Object> pollPackets();
    public void send(Object packet);
}
```

### PacketSender.java

```java
public class PacketSender {
    private static final long MIN_INTERVAL_MS = 50;
    private long lastSendTime = 0;
    
    public void send(Connection conn, Object packet);
    public void move(float x, float y);
    public void lock(int targetId);
    public void attack();
    public void stopAttack();
    public void collect(int boxId);
}
```

---

## Фаза 2: World State (2 дня)

### Задачи
- [x] PlayerState.java
- [x] NpcEntity.java с predictPosition()
- [x] PlayerEntity.java
- [x] BoxEntity.java
- [x] MapInfo.java
- [x] World.java
- [x] WorldSnapshot.java

### PlayerState.java

```java
public class PlayerState {
    private float x, y;
    private int hp, maxHp;
    private int shield, maxShield;
    private float speed;
    private boolean inSafeZone;
    private int cargoUsed, cargoMax;
    
    // Computed
    public float hpPercent();
    public boolean isCargoFull();
    public boolean isHpCritical();
    public float distanceTo(float x, float y);
}
```

### NpcEntity.java

```java
public class NpcEntity {
    private int id;
    private int npcType;
    private float x, y;
    private int hp, maxHp;
    private float speed;
    private long lastUpdateTime;
    private float lastX, lastY;
    
    // Предсказание позиции
    public float[] predictPosition(float seconds);
}
```

---

## Фаза 3: Protocol Layer (2 дня)

### Задачи
- [x] PacketProcessor.java
- [x] Обработка GameStateResponsePacket
- [x] Обработка MapInfoPacket
- [x] Обработка CollectableInPacket
- [x] Обработка MessageResponsePacket

### PacketProcessor.java

```java
public class PacketProcessor {
    private final DiscoveryCollector discovery;
    
    public void process(Object packet, World world) {
        switch (packet) {
            case GameStateResponsePacket p -> processGameState(p, world);
            case MapInfoPacket p -> processMapInfo(p, world);
            case CollectableInPacket p -> processCollectable(p, world);
            case MessageResponsePacket p -> processMessage(p, world);
            default -> log.debug("Unknown packet: {}", packet.getClass());
        }
    }
}
```

---

## Фаза 4: Action Layer (1 день)

### Задачи
- [x] Action.java (sealed interface)
- [x] ActionExecutor.java

### Action.java

```java
public sealed interface Action {
    record Move(float x, float y) implements Action {}
    record Lock(int targetId) implements Action {}
    record Attack() implements Action {}
    record StopAttack() implements Action {}
    record Collect(int boxId) implements Action {}
    record CollectDelayed(int boxId, long executeAt) implements Action {}
    record UseTeleport(int portalId) implements Action {}
    record SwitchShip(int shipId) implements Action {}
}
```

---

## Фаза 5: Brain (3-4 дня)

### Задачи
- [x] BotState.java (enum)
- [x] BotBrain.java (FSM)
- [x] SafetyBrain.java
- [x] CombatBrain.java
- [x] CollectBrain.java
- [x] NavigationBrain.java

### BotBrain.java (основная логика)

```java
public class BotBrain {
    private BotState state = BotState.IDLE;
    
    public List<Action> decide(WorldSnapshot world) {
        List<Action> actions = new ArrayList<>();
        
        // Safety check first
        if (safetyBrain.shouldFlee(world)) {
            transitionTo(BotState.FLEEING);
        }
        
        switch (state) {
            case IDLE -> decideIdle(world, actions);
            case FARMING -> decideFarming(world, actions);
            case FLEEING -> decideFleeing(world, actions);
            case REPAIRING -> decideRepairing(world, actions);
            // ...
        }
        
        return actions;
    }
}
```

### CombatBrain — выбор цели

```java
float scoreTarget(NpcEntity npc, PlayerState player, WorldSnapshot world) {
    float score = 1000;
    
    float dist = player.distanceTo(npc.x(), npc.y());
    score -= dist * 0.1f;
    
    score -= npc.hp() * 0.001f;
    
    // Бонус за боксы рядом с мобом
    int nearbyBoxes = countBoxesNear(npc.x(), npc.y(), 500f, world);
    score += nearbyBoxes * 100;
    
    return score;
}
```

---

## Фаза 6: Game Loop (1 день)

### Задачи
- [x] GameLoop.java
- [x] WuBotApplication.java
- [ ] Интеграционный тест

### GameLoop.java

```java
public class GameLoop implements Runnable {
    private volatile boolean running = true;
    private static final int TICK_INTERVAL_MS = 100;
    
    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            
            tick();
            
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = TICK_INTERVAL_MS - elapsed;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        }
    }
    
    private void tick() {
        List<Object> packets = connection.pollPackets();
        for (Object p : packets) processor.process(p, world);
        
        List<Action> actions = brain.decide(world.snapshot());
        for (Action a : actions) executor.execute(a);
    }
}
```

---

## Фаза 7: Discovery System (3-4 дня)

### Задачи
- [x] PersistenceManager.java
- [x] MapRegistry.java
- [x] PortalGraph.java с BFS поиском пути
- [x] NpcDatabase.java
- [ ] SpawnPointTracker.java (отложено)
- [x] DiscoveryCollector.java
- [x] Интеграция в GameLoop и PacketProcessor

### Подробности в [DISCOVERY.md](./DISCOVERY.md)

---

## Фаза 8: Ship Management (2 дня)

### Задачи
- [x] ShipInfo.java — информация о корабле
- [x] ShipManager.java — управление кораблями
- [x] Состояние SWITCHING_SHIP в BotBrain
- [x] Логика накопления ресурсов (смена корабля при полном cargo)
- [x] Интеграция с PacketProcessor (EquipResponsePacket)

---

## Метрики успеха

| Метрика | Цель |
|---------|------|
| Время между убийствами NPC | < 4 сек |
| Боксы за бой | 2-5 |
| Время реакции на угрозу | < 500ms |
| Uptime | > 1 час |
| Race conditions | 0 |

---

## Чеклист MVP

- [x] Подключение и авторизация
- [x] Фарм NPC (выбор, атака, orbit)
- [x] Параллельный сбор боксов
- [x] Побег при низком HP
- [x] Ремонт в сейф-зоне
- [x] Смена кораблей (накопление)
- [ ] Стабильная работа 1+ час (требует тестирования)

---

## Известные баги

### ВАЖНАЯ ИНФОРМАЦИЯ - Механика игры (НЕ БАГИ!)

⚠️ **HP NPC = 0/0 до лока - это НОРМАЛЬНО, не баг!**

✅ **NPC Shield работает корректно (id=27, id=28)** - Verified 2025-12-17
- Shield парсится при обнаружении NPC
- В бою Shield уменьшается: 560 → 410 → 280 → 141 → 6 → 0
- После Shield=0 начинается урон по HP

Механика игры:
1. HP NPC НЕ ВИДНО до лока (hp=0, maxHp=0)
2. Последовательность: SELECT → LOCK → получаем HP → ATTACK
3. isDead() возвращает true только если maxHp > 0 && hp = 0 (подтверждённая смерть)

Реализовано в:
- CombatBrain.selectTarget() - выбор по расстоянию, не по HP
- CombatBrain.getActions() - state machine: IDLE → LOCK → WAIT_FOR_HP → ATTACK
- Commit: fc88905

### КРИТИЧНЫЕ (блокируют работу)

| # | Описание | Файл | Строка | Статус |
|---|----------|------|--------|--------|
| ~~BUG-001~~ | ~~Бот не видит мобов - NPC не добавляются в World~~ | PacketProcessor.java | 100-106 | **FIXED** (см. логи 2024-12-17) |
| ~~BUG-002~~ | ~~extractNpcType() возможно неверный ParamId~~ | ParamId.java | 11 | **FIXED** (ENTITY_TYPE=42 правильный) |
| BUG-003 | Боксы могут не добавляться в World | PacketProcessor.java | 81-88 | INVESTIGATE |
| ~~BUG-009~~ | ~~Бот застревает из-за несоответствия конфигурации~~ | BotBrain.java, PacketProcessor.java | - | **FIXED** - CONFIG_SWITCHED лог, REPAIRING разбит на фазы. Коммит: 7bb9394 |
| ~~BUG-010~~ | ~~Неверные ParamIds для HP игрока (использовал 24/31 вместо 25/26)~~ | ParamId.java, PacketProcessor.java | 325-344 | **✅ FIXED** - Игрок использует ParamId 25/26 для HP (PLAYER_HP/PLAYER_MAX_HP). Решение: используется правильные константы в PacketProcessor. Подтверждение из логов: Player HP 76000/76000 (100%), Shield 15000/15000. NPC параметры (id=24,31) игнорируются для игрока. Коммиты: 1ff562e, f636fc5, 79e734b (2024-12-17) |

**Детали BUG-009:** ✅ FIXED (2024-12-17)
- Проблема: бот мог застрять из-за несоответствия configId и ожидаемого состояния
- Решение:
  - CONFIG_SWITCHED логирование в PacketProcessor
  - REPAIRING разбит на фазы: REPAIR_ESCAPE → SWITCH_TO_FARM → REPAIR_FARM
  - Каждая фаза проверяет configId перед выполнением действий
  - FLEEING автоматически переключает на ESCAPE_CONFIG

### СРЕДНИЕ

| # | Описание | Файл | Строка | Статус |
|---|----------|------|--------|--------|
| BUG-004 | playerId может быть 0, свой корабль как другой игрок | PacketProcessor.java | 61-63 | INVESTIGATE |
| ~~BUG-005~~ | ~~isDead() true если MaxHP не установлен~~ | NpcEntity.java | 49-51 | **FIXED** (это норма, не баг) |
| BUG-006 | Позиция данных в POSITION может быть float[], не int[] | PacketProcessor.java | 167-170 | INVESTIGATE |

### НИЗКИЕ

| # | Описание | Файл | Строка | Статус |
|---|----------|------|--------|--------|
| BUG-007 | EXPLORING состояние не реализовано (stub) | BotBrain.java | 212-215 | PLANNED |
| BUG-008 | getMapNameById() возвращает null | NavigationBrain.java | 235-238 | PLANNED |

---

## TODO (задачи для исправления)

### Приоритет 1 (Критично - без этого не работает)

- [x] **Добавить DEBUG логирование в PacketProcessor** ✅ DONE
  - ✅ Логировать каждый ship в processShip()
  - ✅ Логировать результат extractNpcType()
  - ✅ Логировать размеры World коллекций после обработки

- [x] **Проверить ParamId.NPC_TYPE = 9** ✅ DONE
  - ✅ Добавить dump всех change.id в extractNpcType()
  - ✅ Сравнить с реальными данными из сервера
  - ✅ **РЕЗУЛЬТАТ**: ENTITY_TYPE (id=42) правильный, значения: 1=игрок, 100=свой игрок

- [ ] **Исправить BUG-009: застревание в FLEEING**
  - Добавить таймаут в SafetyBrain (30 секунд)
  - После таймаута: если в safe zone → IDLE, иначе продолжить FLEEING
  - Альтернатива: проверка inSafeZone, игнорировать низкий HP если safe=true

- [ ] **Проверить формат данных в changes**
  - POSITION может быть float[] вместо int[]
  - Добавить try-catch с логированием типа данных

### Приоритет 2 (Важно для стабильности)

- [ ] **Добавить логирование в CombatBrain.selectTarget()**
  - Логировать количество NPC в world
  - Логировать каждый рассмотренный NPC

- [ ] **Добавить логирование в CollectBrain.findBestBox()**
  - Логировать количество боксов
  - Логировать результат BoxType.isCollectable()

- [ ] **Проверить что playerId устанавливается**
  - Добавить лог при установке playerId
  - Проверить что первый пакет содержит playerId > 0

### Приоритет 3 (Улучшения)

- [ ] **Реализовать EXPLORING состояние**
  - Патрулирование между точками спавна
  - Использование SpawnPointTracker

- [ ] **Реализовать getMapNameById()**
  - Загружать из MapRegistry
  - Кэшировать имена карт

- [ ] **Добавить метрики**
  - Счётчик убитых NPC
  - Счётчик собранных боксов
  - Время в каждом состоянии

---

## Диагностика (для следующего чата)

### Первый шаг - запустить с DEBUG логами:

```bash
# Добавить в logback.xml:
<logger name="com.wubot.protocol.PacketProcessor" level="DEBUG" />
<logger name="com.wubot.brain" level="DEBUG" />

# Или через системное свойство:
java -Dlogback.debug=true -jar wubot.jar
```

### Что искать в логах:

1. `GameStateResponsePacket: ships=X` - количество кораблей
2. `processShip: npcType=X` - есть ли NPC_TYPE > 0
3. `World state: npcs=X` - добавились ли NPC в World
4. `selectTarget: X NPCs available` - видит ли CombatBrain NPC

### Если npcType всегда 0:

Добавить в extractNpcType():
```java
for (ChangedParameter c : changes) {
    log.debug("Change: id={} type={} data={}", c.id, c.type, c.data);
}
```

И найти какой реальный ID используется для NPC_TYPE.
