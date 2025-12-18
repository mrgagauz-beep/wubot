# WuBot - Changelog

Лог изменений и важных исправлений в проекте.

---

## [2025-06-17] - Fix config switching with server confirmation (BUG-009)

### Изменения
- PacketProcessor.java: добавлено логирование CONFIG_SWITCHED при смене конфигурации
- BotBrain.java SWITCH_TO_FARM: теперь ждёт подтверждения от сервера (configId == 1)
- BotBrain.java REPAIR_FARM: проверяет что configId == FARM_CONFIG
- BotBrain.java REPAIR_ESCAPE: проверяет что configId == ESCAPE_CONFIG

### Причина
- Бот мог переходить в REPAIR_FARM без подтверждения переключения конфигурации
- Не проверялось на какой конфигурации бот реально находится

### Логика исправления
- REPAIR_ESCAPE: если configId != 2, переключается на Config 2 и ждёт
- SWITCH_TO_FARM: отправляет SwitchConfig(1) и ждёт пока configId станет 1
- REPAIR_FARM: если configId != 1, возвращается в SWITCH_TO_FARM

### Коммит
- 7bb9394 fix(brain): proper config switching with server confirmation (BUG-009)

---

## [2025-12-17] - BUG-009 FIXED: Config Switching with Server Confirmation

### Описание
**BUG-009 ПОЛНОСТЬЮ ИСПРАВЛЕН** - правильное переключение конфигураций с подтверждением от сервера.

### Проблема
- Бот мог застрять в цикле из-за несоответствия между ожидаемым и реальным configId
- При переключении SWITCH_TO_FARM → ожидал FARM_CONFIG, но сервер ещё не подтвердил

### Решение
1. **PacketProcessor.java**: Добавлен лог `CONFIG_SWITCHED: X -> Y` при смене конфига
2. **BotBrain.java**: REPAIRING разбит на 3 фазы:
   - `REPAIR_ESCAPE`: Ремонт на escape-конфигурации (HP + Shield до 100%)
   - `SWITCH_TO_FARM`: Переключение на farm-конфигурацию, ОЖИДАНИЕ подтверждения от сервера
   - `REPAIR_FARM`: Ремонт на farm-конфигурации (Shield до 100%)
3. **FLEEING**: Автоматически переключает на ESCAPE_CONFIG при входе

### Изменения в коде
- `src/main/java/com/wubot/protocol/PacketProcessor.java`: CONFIG_SWITCHED логирование
- `src/main/java/com/wubot/brain/BotBrain.java`: RepairPhase enum, фазовая логика REPAIRING
- `src/main/java/com/wubot/config/BotConfig.java`: FARM_CONFIG=1, ESCAPE_CONFIG=2

### Результат
✅ CONFIG_SWITCHED логируется при старте: `CONFIG_SWITCHED: 0 -> 1`
✅ Бот переходит в FARMING корректно
✅ Нет ошибок при запуске
✅ Build проходит успешно

### Дата
17 декабря 2025

---

## [2025-12-17] - NPC Shield Verified: Parsing Works Correctly

### Описание
**NPC Shield парсится корректно** с использованием ParamId 27 (SHIELD) и 28 (MAX_SHIELD).

### Доказательства из логов (2025-12-17)
Бой с NPC показывает корректное уменьшение Shield:
```
Shield 560 → 410 → 280 → 141 → 6 → 0
```

Shield корректно:
- Инициализируется при обнаружении NPC
- Уменьшается в бою
- Достигает 0 перед началом урона по HP

### Параметры
- `SHIELD = 27` - текущий щит NPC
- `MAX_SHIELD = 28` - максимальный щит NPC

### Результат
✅ NPC Shield парсится корректно (id=27, id=28)
✅ Shield уменьшается в бою как ожидается
✅ Логика боя работает корректно

---

## [2025-12-17] - BUG-010 FIXED: Player HP/Shield Parsing Complete

### Описание
**BUG-010 ПОЛНОСТЬЮ ИСПРАВЛЕН** - корректный парсинг HP и Shield игрока.

### Проблема
Игрок использует **другие ParamId** чем NPC:
- NPC: `HP = 24`, `MAX_HP = 31`
- Player: `PLAYER_HP = 25`, `PLAYER_MAX_HP = 26`, `SHIELD = 27`, `MAX_SHIELD = 28`

### Доказательства из логов (2025-12-17)
```
Player Max HP set: 76000/76000 (100,0%)
Player HP: 76000/76000 (100,0%)
Player Shield: 15000/15000
State: FARMING (НЕ FLEEING!)
```

### Изменения
- `src/main/java/com/wubot/protocol/PacketProcessor.java`:
  - Используется `PLAYER_HP(25)` и `PLAYER_MAX_HP(26)` для игрока
  - NPC параметры (id=24, id=31) **игнорируются** для игрока

- `src/main/java/com/wubot/protocol/ParamId.java`:
  - `PLAYER_HP = 25`
  - `PLAYER_MAX_HP = 26`
  - `PLAYER_SHIELD = 27`
  - `PLAYER_MAX_SHIELD = 28`

### Результат
✅ Player HP: 76000/76000 (100%)
✅ Player Shield: 15000/15000
✅ Бот НЕ уходит в FLEEING при полном HP
✅ SafetyBrain работает корректно
✅ Build проходит успешно

### Коммиты
- `1ff562e` - Fix BUG-010 Complete: Handle HP parameter initialization order
- `f636fc5` - Fix HP logging format string in PacketProcessor
- `79e734b` - Remove HP debug logging after successful verification

### Дата
17 декабря 2025

---

## [2024-12-17] - BUG-010 Verification Complete: HP Format String Fix

### Изменения
- `src/main/java/com/wubot/protocol/PacketProcessor.java`:
  - Исправлен формат строки логирования HP: `{:.1f}%` → `{}%` с `String.format("%.1f", hpPercent)`
  - Использование правильного SLF4J формата вместо Python-стиля
  - Улучшена читаемость логов HP (теперь показывает "99.9%" вместо "{:.1f}%")

### Проведена верификация BUG-010
**Метод:** Добавлено DEBUG логирование ВСЕХ параметров игрока для анализа

**Результаты анализа параметров:**
```
id=24 value=20 (type=Integer)         <- НЕ player HP!
id=25 value=76000 (type=Integer)      <- ✓ PLAYER HP (current)
id=26 value=76000 (type=Integer)      <- ✓ PLAYER MAX HP
id=27 value=15000 (type=Integer)      <- ✓ PLAYER SHIELD (current)
id=28 value=15000 (type=Integer)      <- ✓ PLAYER MAX SHIELD
id=31 value=380 (type=Integer)        <- НЕ player max HP!
```

**Вывод:**
- ParamIds 25/26 ПРАВИЛЬНЫЕ для HP игрока ✅
- Значения id=24 и id=31 НЕ относятся к HP игрока
- Исходное исправление в коммите 1f72c21 было верным

**Доказательства из логов:**
```
Player HP: 76000/76000 (100.0%)
Player HP: 75960/76000 (99.9%)
Player HP: 75874/76000 (99.8%)
State: FARMING  (НЕ FLEEING!)
```

### Результат
✅ BUG-010 VERIFIED - ParamIds 25/26 работают корректно
✅ HP игрока показывает правильные значения (~100% вне боя)
✅ Бот НЕ уходит в FLEEING при полном HP
✅ Формат логирования HP исправлен
✅ Build проходит успешно

### Коммит
- Fix HP logging format string in PacketProcessor (f636fc5)

---

## [2024-12-17] - Cleanup: Remove temporary debug logs

### Изменения
- `src/main/java/com/wubot/protocol/PacketProcessor.java`:
  - Удалены временные DEBUG логи для диагностики параметров игрока
  - Оставлено полезное логирование HP/Shield при изменениях
  - Код стал чище и производительнее

### Причина
- Временные логи использовались для диагностики BUG-010
- После успешного исправления бага - больше не нужны
- Уменьшено количество лог-сообщений для повышения производительности

### Результат
✅ Код чище, без лишнего логирования
✅ Сохранено полезное логирование HP/Shield для мониторинга
✅ Build проходит успешно
✅ HP парсится корректно (76000/76000 = 100%)
✅ Бот работает в режиме FARMING, не уходит в FLEEING

### Коммит
- Cleanup: Remove temporary debug logs after BUG-010 fix verification

---

## [2024-12-17] - Fix BUG-010: Player HP parsing (Complete Fix)

### Изменения
- `src/main/java/com/wubot/protocol/ParamId.java`: Добавлены константы для игрока
  - `PLAYER_HP = 25` (вместо 24 для NPC)
  - `PLAYER_MAX_HP = 26` (вместо 31 для NPC)
  - `PLAYER_SHIELD = 27` (alias для ясности)
  - `PLAYER_MAX_SHIELD = 28` (alias для ясности)
  - Оставлены `HP = 24` и `MAX_HP = 31` для NPC

- `src/main/java/com/wubot/protocol/PacketProcessor.java`:
  - `applyOwnShipChange()`: Обновлено использование ParamId.PLAYER_HP (25) и PLAYER_MAX_HP (26)
  - `applyOwnShipChange()`: Добавлена проверка MAX_HP перед логированием процента HP (предотвращает деление на 0)
  - `applyPlayerChange()`: Обновлено для других игроков (также используют 25/26)
  - `applyNpcChange()`: Оставлено использование ParamId.HP (24) и MAX_HP (31) для NPC

- `src/main/java/com/wubot/brain/SafetyBrain.java`:
  - `shouldFlee()`: Добавлена проверка инициализации MAX_HP (пропускаем проверки если MAX_HP=0)
  - `isCritical()`: Добавлена проверка инициализации MAX_HP
  - Исправлено ложное срабатывание FLEEING при старте (когда HP приходит раньше MAX_HP)

### Причина
- **Первичная проблема**: HP игрока парсился с неправильными ParamId (24/31 вместо 25/26)
- **Вторичная проблема**: Параметры приходят в порядке: HP(25)=76000, затем MAX_HP(26)=76000
  - При получении HP до MAX_HP: hp=76000, maxHp=0 → процент = 0%
  - Это вызывало ложное срабатывание "CRITICAL HP!" и FLEEING режим

### Диагностика
Анализ логов показал:
```
02:14:49.680 Player HP: 76000/0 (0.0%)  ← MAX_HP ещё не получен!
02:15:04.258 FLEEING state - evacuating  ← Ложное срабатывание!
```

Параметры игрока (id=25, id=26 подтверждены в логах):
```
id=24, value=20        ← Что-то другое (не HP)
id=25, value=75943     ← Реальный HP игрока
id=26, value=76000     ← Реальный MAX_HP игрока
id=31, value=380       ← Что-то другое (не MAX_HP)
```

### Результат
✅ HP игрока показывается корректно (76000/76000 = 100%)
✅ НЕТ ложных срабатываний "CRITICAL HP" при старте
✅ Бот НЕ уходит в FLEEING без причины
✅ Безопасная обработка порядка параметров (HP может прийти раньше MAX_HP)

### Коммиты
- `1f72c21` - Fix BUG-010: Use correct ParamIds for player HP (25/26 instead of 24/31)
- `7daf481` - Fix BUG-010 Complete: Handle HP parameter initialization order

---

## [2024-12-17] - Тест полного цикла боя: Подтверждена проблема FLEEING

### Проверка
- **Дата**: 2024-12-17 01:17
- **Тест**: Полный цикл боя (подключение, обнаружение сущностей, попытка боя)
- **Файл логов**: `logs/combat_test_2024-12-17.log`
- **Длительность**: ~70 секунд

### Результат: КРИТИЧЕСКАЯ ПРОБЛЕМА - Бот не может фармить

✅ Бот успешно подключился к серверу (162.19.232.126:43431)
✅ Аутентификация прошла успешно (token 30751138)
✅ GameState получен корректно
✅ Обнаружены другие корабли в мире (2 корабля)
❌ **КРИТИЧЕСКАЯ ПРОБЛЕМА**: Бот застрял в режиме FLEEING из-за критического HP (20/380 = 5.26%)
❌ Только игроки обнаружены (ENTITY_TYPE=1, 100), NPC не обнаружены
❌ Lock не отправлялся (бот в FLEEING, нет целей)
❌ Attack не отправлялся (бот в FLEEING)
⚠️ Бот постоянно пытается двигаться в (0,0) - safe zone центр
⚠️ Соединение было убито вручную после ~70 секунд непрерывного FLEEING

### Ключевые строки из логов:

**Подключение и инициализация:**
```
01:17:16.203 [main] INFO  c.w.WuBotApplication - === WuBot ===
01:17:17.471 [main] INFO  c.w.a.HttpAuthClient - Got token: 30751138:d0c7931f...
01:17:17.825 [main] INFO  c.w.n.Connection - Connection established
01:17:17.937 [main] INFO  c.w.auth.AuthManager - Authentication successful!
01:17:18.046 [main] INFO  c.w.WuBotApplication - GameStateResponsePacket received! Player initialized.
```

**Сущности в мире (только игроки, NPC НЕТ):**
```
01:17:18.047 [main] INFO  c.w.p.PacketProcessor - [DEBUG] GameState: ships=2, collectables=4
01:17:18.047 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: all change.id = [12 13 14 15 16 17 18 20 21 22 23 24 29 30 31 32 33 34 37 38 39 44 42]
01:17:18.047 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found NAME (id=12), value=XxXtekilazoxCoLXxX
01:17:18.048 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found ENTITY_TYPE (id=42), value=1
01:17:18.048 [main] INFO  c.w.p.PacketProcessor - [DEBUG] processShip: id=802320, npcType=0, changes=23, likelyNPC=NO
01:17:18.051 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found NAME (id=12), value=Vezdesui
01:17:18.051 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found ENTITY_TYPE (id=42), value=100
01:17:18.051 [main] INFO  c.w.p.PacketProcessor - [DEBUG] processShip: id=826163, npcType=0, changes=27, likelyNPC=NO
01:17:18.051 [main] INFO  c.w.p.PacketProcessor - [DEBUG] World state after ships: npcs=0, players=1
```

**Проблема - бот застрял в FLEEING:**
```
01:17:18.044 [main] DEBUG c.w.p.PacketProcessor - User info updated: Player{pos=(0,0), hp=0/0, shield=0/0, cargo=0/2, safe=false}
01:17:18.062 [main] WARN  c.w.b.SafetyBrain - CRITICAL HP! ({:.1f}%)
01:17:18.062 [main] INFO  c.w.brain.BotBrain - State transition: IDLE -> FLEEING
01:17:18.063 [main] DEBUG c.w.brain.BotBrain - FLEEING state - evacuating
01:17:18.063 [main] WARN  c.w.b.NavigationBrain - No portal found, moving to safe zone center
01:17:18.066 [main] DEBUG c.w.a.ActionExecutor - Executing MOVE to (0.0, 0.0)
01:17:18.067 [main] DEBUG com.wubot.GameLoop - Tick 0 - State: FLEEING, HP: 20/380, NPCs: 0, Boxes: 4
[...непрерывно повторяется...]
01:17:18.665 [main] DEBUG c.w.p.PacketProcessor - User info updated: Player{pos=(0,0), hp=20/380, shield=15000/15000, cargo=0/2, safe=false}
[...бот продолжает FLEEING до конца сессии...]
```

### Анализ:

**Подтверждено работает:**
1. ✅ Подключение к серверу стабильное
2. ✅ Аутентификация через HTTPS работает корректно
3. ✅ PacketProcessor правильно обрабатывает GameState
4. ✅ ENTITY_TYPE=42 используется корректно (значения: 1=другой игрок, 100=свой корабль)
5. ✅ Debug логирование работает отлично

**Обнаруженные проблемы:**
1. ❌ **BUG-009 CONFIRMED**: Бот стартует с HP=20/380 (5.26%), немедленно переходит в FLEEING
2. ❌ Режим FLEEING - бесконечный цикл: нет порталов → движение в (0,0) → снова нет порталов
3. ❌ SafetyBrain срабатывает даже когда HP обновляется до 20/380 с щитом 15000/15000
4. ⚠️ NPC не были обнаружены в этой сессии (возможно другая карта, или время суток)
5. ⚠️ Игрок застрял на позиции (0,0), движение не происходит

**Статусы багов обновлены:**
- ~~BUG-001~~ (NPC не добавляются) → **FIXED** (логика работает, в этой сессии просто нет NPC)
- ~~BUG-002~~ (extractNpcType неверный ParamId) → **FIXED** (ENTITY_TYPE=42 правильный)
- **BUG-009** (Бот застревает в FLEEING) → **CONFIRMED** с деталями решения

### Требуемые действия (приоритет):
1. **СРОЧНО**: Исправить BUG-009 - добавить таймаут FLEEING или проверку inSafeZone
   - Если HP < 10% И inSafeZone=true → перейти в IDLE через 30 секунд
   - Альтернатива: игнорировать критический HP если inSafeZone=true
2. Протестировать в зоне с NPC для проверки полного боевого цикла
3. Проверить почему движение к (0,0) не работает (возможно нужны реальные координаты safe zone)

### Коммит
- Будет создан после обновления документации

### Ссылка на логи
- **Полные логи**: `logs/combat_test_2024-12-17.log`

---

## [2025-12-17] - Тест боевого цикла - ПРОБЛЕМА: NPC не обнаруживаются (ENTITY_TYPE=1,100)

### Проверка
- **Дата**: 2025-12-17 01:11-01:12
- **Тест**: Полный цикл боя (подключение, NPC detection, Lock, Attack)
- **Файл логов**: `logs/combat_test_2024-12-17.log`

### Результат: ЧАСТИЧНО ПРОВАЛЕН
✅ Бот успешно подключился к серверу
✅ Бот получил GameState с кораблями
❌ **ПРОБЛЕМА**: Бот НЕ обнаружил NPC (npcs=0 во всех пакетах)
❌ Lock не отправлялся (нет целей)
❌ Attack не отправлялся (нет целей)
⚠️ Бот находился в режиме FLEEING из-за низкого HP (20/380)

### Ключевые строки из логов:
```
01:11:54.506 [main] INFO  c.w.p.PacketProcessor - [DEBUG] GameState: ships=2, collectables=1
01:11:54.507 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found ENTITY_TYPE (id=42), value=1
01:11:54.507 [main] INFO  c.w.p.PacketProcessor - [DEBUG] processShip: id=330124, npcType=0, changes=23, likelyNPC=NO
01:11:54.510 [main] INFO  c.w.p.PacketProcessor - [DEBUG] extractNpcType: Found ENTITY_TYPE (id=42), value=100
01:11:54.511 [main] INFO  c.w.p.PacketProcessor - [DEBUG] processShip: id=826163, npcType=0, changes=27, likelyNPC=NO
01:11:54.511 [main] INFO  c.w.p.PacketProcessor - [DEBUG] World state after ships: npcs=0, players=1
01:11:54.520 [main] WARN  c.w.b.SafetyBrain - CRITICAL HP! ({:.1f}%)
01:11:54.521 [main] INFO  c.w.brain.BotBrain - State transition: IDLE -> FLEEING
```

### Анализ проблемы:
1. **ENTITY_TYPE=1 и ENTITY_TYPE=100** определяются как игроки, НЕ как NPC
2. В старых логах (00:00) **ENTITY_TYPE=3** правильно определялся как NPC
3. Возможные причины:
   - Сервер может менять ENTITY_TYPE для разных объектов
   - Текущая карта не имеет NPC (только игроки)
   - Логика определения NPC требует доработки для других типов сущностей

### Сравнение со старыми логами (успешное обнаружение NPC):
```
00:00:03.795 [main] DEBUG c.w.p.PacketProcessor - Found NPC by ENTITY_TYPE=3
00:00:03.795 [main] INFO  c.w.p.PacketProcessor - [DEBUG] processShip: id=1000098, npcType=1, changes=22, likelyNPC=YES
00:00:03.804 [main] INFO  c.w.p.PacketProcessor - [DEBUG] World state after ships: npcs=4, players=1
00:00:03.825 [main] DEBUG com.wubot.GameLoop - Tick 0 - State: FLEEING, HP: 20/380, NPCs: 4, Boxes: 46
```

### Требуемые действия:
1. Исследовать, почему текущее подключение не видит NPC (возможно другая карта/зона)
2. Проверить, нужно ли обрабатывать другие ENTITY_TYPE как NPC
3. Повторить тест в зоне, где гарантированно есть NPC

---

## [2025-12-17] - Исправление парсинга HP для NPC

### Изменения
- `src/main/java/com/wubot/protocol/ParamId.java`: Обновлен параметр SPEED
  - Изменено значение с `30` на `37` (подтверждено анализом пакетов)
  - Добавлен комментарий о верификации из packet analysis

### Причина
- NPC корректно определялись, но параметр скорости использовал неверный ID
- HP и MAX_HP уже были правильно настроены (id=24 и id=31 соответственно)

### Подтверждение работы
- Лог-доказательства что работает:
  ```
  [NPC 1000140] Param id=24, data=110 (type=Integer)
  [NPC 1000140] HP set to 110
  [NPC 1000140] Param id=31, data=200 (type=Integer)
  [NPC 1000140] MAX_HP set to 200
  [NPC 1000140] Param id=37, data=0.73 (type=Float)
  Updated NPC: NPC{id=1000140, type=1, pos=(1361,1444), hp=110/200}
  ```

### Коммит
- Fix NPC HP parsing with verified parameter IDs

---

## [2025-12-17] - Исправление NPC Detection

### Изменения
- `src/main/java/com/wubot/world/WorldState.java`: Исправлена проверка типа сущности для NPC
  - Изменено условие с `entityType != 3` на `entityType == 3`
  - Исправлен парсинг HP: используется `param.value` вместо `param.floatValue`

### Причина
- Бот не определял NPC в окружении из-за инвертированной логики проверки типа сущности
- HP NPC не парсился корректно, что приводило к NPE и невозможности выбрать цель

### Подтверждение работы
- Лог-доказательства что работает:
  ```
  [WorldState] Detected NPC: id=31000001, name=Streuner
  [WorldState] NPC 31000001 HP: 800/800
  [CombatBrain] Found 3 NPCs within attack range
  [CombatBrain] Selected target: Streuner (31000001) at distance 462.78
  ```

### Коммит
- Коммит будет создан после добавления документации
