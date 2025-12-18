# WuBot - Claude Code Instructions

## Проект
Java бот для игры WarUniverse с FSM архитектурой.

## Технологии
- Java 17
- Gradle 8+
- KryoNet (сетевой слой)
- Gson (JSON)
- SLF4J + Logback (логирование)

## Структура проекта
```
src/main/java/com/wubot/
├── WuBotApplication.java    # Точка входа
├── GameLoop.java            # Главный цикл (10 Hz)
├── network/                 # Сетевой слой (KryoNet)
├── protocol/                # Обработка пакетов
├── world/                   # Состояние мира (Entity, WorldState)
├── brain/                   # AI и FSM (BrainFSM, состояния)
├── action/                  # Действия (Attack, Collect, Move)
├── discovery/               # Автокартографирование
├── ship/                    # Управление кораблями
├── config/                  # Конфигурация
└── util/                    # Утилиты
```

## Ключевые команды
```bash
# Сборка
.\gradlew build

# Запуск бота
.\gradlew run

# Запуск с таймаутом (для тестов)
timeout 20 .\gradlew run
```

## Правила работы

### 1. ДВОЙНОЕ ПОДТВЕРЖДЕНИЕ ДЕЙСТВИЙ
Каждое игровое действие требует ДВА подтверждения в логах:
1. **Пакет от сервера** — входящий пакет с подтверждением
2. **Обработка ботом** — лог что бот знает о результате

Пример для атаки:
```
[DEBUG] <- Server: AttackConfirm targetId=123
[INFO] Bot: Now attacking NPC 'Streuner' (id=123)
```

### 2. АТОМАРНЫЕ ИЗМЕНЕНИЯ
- Одно изменение за раз
- Каждое изменение = коммит
- Проверка через `.\gradlew build` после каждого изменения

### 3. ЛОГИРОВАНИЕ
- Все действия логируются
- Формат: `[STATE] Action: description`
- Состояния FSM: IDLE, HUNTING, ATTACKING, COLLECTING, FLEEING, REPAIRING

## Важные константы
```java
TICK_INTERVAL_MS = 100;      // 10 Hz game loop
PACKET_MIN_INTERVAL_MS = 50; // Rate limiting
FLEE_HP_PERCENT = 0.30f;     // Порог побега (30% HP)
Y_OFFSET = 97f;              // Смещение для сбора боксов
COLLECT_RADIUS = 800f;       // Радиус сбора
```

## Важные ParamId
```java
ENTITY_TYPE = 42;    // 3 = NPC, 100 = Player
NAME = 12;           // Имя сущности
PLAYER_HP = 25;      // HP игрока
PLAYER_MAX_HP = 26;  // Макс HP игрока
HP = 24;             // HP NPC
MAX_HP = 31;         // Макс HP NPC
POSITION = 17;       // [x, y]
```

## Документация
- `docs/ARCHITECTURE.md` — архитектура
- `docs/PACKETS.md` — протокол пакетов
- `docs/IMPLEMENTATION_PLAN.md` — план и статус
- `docs/CHANGELOG.md` — история изменений

## После каждого задания
1. `.\gradlew build` — проверка сборки
2. Коммит изменений
3. Обновление docs/CHANGELOG.md
4. Обновление docs/IMPLEMENTATION_PLAN.md (если нужно)
