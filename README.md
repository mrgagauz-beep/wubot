# WuBot - WarUniverse Bot

Автоматический бот для игры WarUniverse с самообучением.

## Особенности

- **Один поток** — никаких race conditions
- **Параллельные действия** — атака и сбор одновременно
- **FSM архитектура** — предсказуемое поведение
- **Discovery System** — автоматический сбор информации о мире
- **Накопление ресурсов** — на нескольких кораблях

## Требования

- Java 17+
- Gradle 8+

## Сборка

```bash
./gradlew build
```

## Запуск

```bash
./gradlew run
```

## Документация

- [Архитектура](docs/ARCHITECTURE.md)
- [Протокол пакетов](docs/PACKETS.md)
- [План реализации](docs/IMPLEMENTATION_PLAN.md)
- [Discovery System](docs/DISCOVERY.md)

## Структура

```
src/main/java/com/wubot/
├── network/      # Сетевой слой
├── protocol/     # Обработка пакетов
├── world/        # Состояние мира
├── discovery/    # Автокартографирование
├── brain/        # AI и FSM
├── action/       # Действия
└── config/       # Конфигурация
```

## Статус

🚧 В разработке

## Лицензия

Private

---
*Last verified: December 2025*
