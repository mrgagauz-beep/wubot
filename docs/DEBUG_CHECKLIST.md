# WuBot - Debug Checklist

## Чеклист отладки проблемы "бот не видит мобов"

### Этап 1: Проверка получения пакетов

- [ ] **Проверить что GameStateResponsePacket приходит**
  ```java
  // PacketProcessor.java:39
  if (packet instanceof GameStateResponsePacket p) {
      log.info("=== GameStateResponsePacket received ===");
      log.info("playerId={}, safeZone={}", p.playerId, p.safeZone);
      log.info("ships count: {}", p.ships != null ? p.ships.length : 0);
      log.info("collectables count: {}", p.collectables != null ? p.collectables.length : 0);
      processGameState(p, world);
  }
  ```

- [ ] **Проверить что ships[] не пустой**
  ```java
  // PacketProcessor.java:74-78
  if (packet.ships != null) {
      log.info("Processing {} ships", packet.ships.length);
      for (GameStateResponsePacket.ShipInResponse ship : packet.ships) {
          log.debug("Ship: {}", ship);  // toString() уже реализован
          processShip(ship, world);
      }
  }
  ```

### Этап 2: Проверка распознавания NPC

- [ ] **Проверить что ships[] содержит NPC (npcType > 0)**
  ```java
  // PacketProcessor.java:95-99
  private void processShip(GameStateResponsePacket.ShipInResponse ship, World world) {
      log.debug("processShip: id={}, changes={}", ship.id,
          ship.changes != null ? ship.changes.length : "null");

      if (ship.changes == null || ship.changes.length == 0) {
          log.trace("Ship {} has no changes", ship.id);
          return;
      }
      // ...
  }
  ```

- [ ] **Проверить extractNpcType() находит NPC_TYPE**
  ```java
  // PacketProcessor.java:131-138
  private int extractNpcType(ChangedParameter[] changes) {
      log.trace("extractNpcType: checking {} changes", changes.length);
      for (ChangedParameter change : changes) {
          log.trace("  Change: id={}, type={}, data={}",
              change.id, change.type, change.data);
          if (change.id == ParamId.NPC_TYPE && change.data != null) {
              int npcType = ((Number) change.data).intValue();
              log.debug("Found NPC_TYPE={}", npcType);
              return npcType;
          }
      }
      log.trace("No NPC_TYPE found in changes");
      return 0;
  }
  ```

- [ ] **Проверить ParamId.NPC_TYPE = 9 корректно**
  - Сравнить с реальными данными из пакетов
  - Если change.id != 9 для NPC, найти правильное значение

### Этап 3: Проверка заполнения World

- [ ] **Проверить что World.npcs заполняется**
  ```java
  // PacketProcessor.java:143-158
  private void processNpc(GameStateResponsePacket.ShipInResponse ship, int npcType, World world) {
      log.info("=== PROCESSING NPC: id={}, type={} ===", ship.id, npcType);

      NpcEntity npc = world.getOrCreateNpc(ship.id);
      boolean isNew = npc.getNpcType() == 0;
      log.debug("NPC isNew={}", isNew);
      // ...
  }
  ```

- [ ] **Проверить размер коллекций после обработки**
  ```java
  // После цикла processShip в processGameState
  log.info("World state after processing: npcs={}, players={}, boxes={}",
      world.getAllNpcs().size(),
      world.getAllPlayers().size(),
      world.getAllBoxes().size());
  ```

### Этап 4: Проверка WorldSnapshot

- [ ] **Проверить что WorldSnapshot содержит NPC**
  ```java
  // WorldSnapshot.java конструктор
  public WorldSnapshot(World world) {
      // ...
      this.npcs = Collections.unmodifiableList(new ArrayList<>(world.getAllNpcs()));
      log.debug("WorldSnapshot created: npcs={}, boxes={}", npcs.size(), boxes.size());
      // ...
  }
  ```

### Этап 5: Проверка CombatBrain

- [ ] **Проверить что CombatBrain.selectTarget() находит цели**
  ```java
  // CombatBrain.java:87-102
  public NpcEntity selectTarget(WorldSnapshot world) {
      log.debug("selectTarget: {} NPCs available", world.getNpcs().size());

      NpcEntity bestTarget = null;
      float bestScore = Float.NEGATIVE_INFINITY;

      for (NpcEntity npc : world.getNpcs()) {
          log.trace("Evaluating NPC: id={}, type={}, hp={}/{}, dead={}",
              npc.getId(), npc.getNpcType(), npc.getHp(), npc.getMaxHp(), npc.isDead());

          if (npc.isDead()) {
              log.trace("  Skipped: dead");
              continue;
          }
          // ...
      }

      log.debug("selectTarget result: {}", bestTarget);
      return bestTarget;
  }
  ```

### Этап 6: Проверка CollectBrain (боксы)

- [ ] **Проверить что CollectBrain видит боксы**
  ```java
  // CollectBrain.java:89-118
  public BoxEntity findBestBox(WorldSnapshot world, float maxRadius, boolean cargoFull) {
      log.debug("findBestBox: {} boxes available, maxRadius={}, cargoFull={}",
          world.getBoxes().size(), maxRadius, cargoFull);

      for (BoxEntity box : world.getBoxes()) {
          log.trace("Evaluating box: id={}, type={}, pos=({},{})",
              box.getId(), box.getBoxType(), box.getX(), box.getY());
          // ...
      }
  }
  ```

### Этап 7: Проверка FSM

- [ ] **Проверить состояние FSM (какой state, почему не меняется)**
  ```java
  // BotBrain.java:46-69
  public List<Action> decide(WorldSnapshot world) {
      log.debug("=== BotBrain.decide() state={} ===", state);
      // ...
  }

  // BotBrain.java:88-122
  private void decideFarming(WorldSnapshot world, List<Action> actions) {
      log.debug("decideFarming: world has {} npcs, {} boxes",
          world.getNpcs().size(), world.getBoxes().size());

      List<Action> combatActions = combatBrain.getActions(world);
      log.debug("Combat actions: {}", combatActions.size());

      List<Action> collectActions = collectBrain.getActions(world);
      log.debug("Collect actions: {}", collectActions.size());
  }
  ```

---

## Ключевые места для логирования

### PacketProcessor.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 39 | process() | Тип пакета, количество ships/collectables |
| 74-78 | processGameState() | Количество кораблей перед обработкой |
| 95-99 | processShip() | ship.id, количество changes |
| 100-112 | processShip() | npcType после extractNpcType() |
| 131-138 | extractNpcType() | Каждый change.id в массиве |
| 143 | processNpc() | ship.id, npcType, isNew |
| 307-320 | processCollectable() | box.id, type, existOnMap |

### World.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 40-41 | getOrCreateNpc() | id, isNew (computeIfAbsent) |
| 84-85 | getOrCreateBox() | id, isNew |

### WorldSnapshot.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 53-55 | constructor | Размеры коллекций после копирования |

### BotBrain.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 46 | decide() | Текущий state |
| 88 | decideFarming() | Размеры world.getNpcs(), world.getBoxes() |
| 109-122 | decideFarming() | combatActions.size(), collectActions.size() |

### CombatBrain.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 29 | getActions() | world.getNpcs().size() |
| 87-102 | selectTarget() | Каждый NPC и его score |

### CollectBrain.java

| Строка | Метод | Что логировать |
|--------|-------|----------------|
| 37 | getActions() | world.getBoxes().size(), cargoFull |
| 89-118 | findBestBox() | Каждый бокс, его тип, дистанция |

---

## Быстрый Debug Mode

Для включения подробного логирования добавьте в `logback.xml`:

```xml
<logger name="com.wubot.protocol.PacketProcessor" level="DEBUG" />
<logger name="com.wubot.brain.CombatBrain" level="DEBUG" />
<logger name="com.wubot.brain.CollectBrain" level="DEBUG" />
<logger name="com.wubot.brain.BotBrain" level="DEBUG" />
<logger name="com.wubot.world" level="DEBUG" />
```

Или через командную строку:
```bash
java -Dlogback.debug=true -jar wubot.jar
```

---

## Код для копирования (DEBUG логи)

### Вариант 1: Минимальный набор логов

```java
// PacketProcessor.java - в начало processShip()
log.info("[DEBUG] processShip: id={}, changes={}, npcType={}",
    ship.id,
    ship.changes != null ? ship.changes.length : "null",
    extractNpcType(ship.changes));

// CombatBrain.java - в начало selectTarget()
log.info("[DEBUG] selectTarget: {} NPCs in world", world.getNpcs().size());
for (NpcEntity npc : world.getNpcs()) {
    log.info("[DEBUG]   NPC: id={}, type={}, hp={}/{}",
        npc.getId(), npc.getNpcType(), npc.getHp(), npc.getMaxHp());
}
```

### Вариант 2: Полный dump changes

```java
// PacketProcessor.java - в extractNpcType()
private int extractNpcType(ChangedParameter[] changes) {
    StringBuilder sb = new StringBuilder("[DEBUG] changes dump:\n");
    for (ChangedParameter c : changes) {
        sb.append(String.format("  id=%d, type=%d, data=%s (%s)\n",
            c.id, c.type, c.data,
            c.data != null ? c.data.getClass().getSimpleName() : "null"));
    }
    log.info(sb.toString());

    // original code...
}
```

---

## Ожидаемый результат после отладки

После исправления проблем логи должны показывать:

```
INFO  [PacketProcessor] GameStateResponsePacket: playerId=12345, ships=15
INFO  [PacketProcessor] processShip: id=99001, npcType=3 (NPC!)
INFO  [PacketProcessor] processShip: id=99002, npcType=3 (NPC!)
INFO  [PacketProcessor] World state: npcs=10, players=2, boxes=5
INFO  [CombatBrain] selectTarget: 10 NPCs available
INFO  [CombatBrain] Selected target: NPC{id=99001, type=3, hp=50000/50000}
INFO  [BotBrain] State: FARMING, actions: [Lock(99001), Attack(), Move(1234, 5678)]
```
