# WarUniverse - Индекс игровых механик

## Обзор

WarUniverse — космическая MMO-игра, похожая на DarkOrbit. Игроки управляют космическими кораблями, сражаются с инопланетянами (NPC), участвуют в PvP, собирают ресурсы и прокачивают персонажа.

---

## Список механик

| # | Механика | Файл | Статус | Приоритет для бота |
|---|----------|------|--------|-------------------|
| 1 | [Combat System (Боевая система)](./mechanics/01_COMBAT.md) | 01_COMBAT.md | ✅ Готово | 🔴 Высокий |
| 2 | [Ships (Корабли)](./mechanics/02_SHIPS.md) | 02_SHIPS.md | ✅ Готово | 🔴 Высокий |
| 3 | [Equipment (Экипировка)](./mechanics/03_EQUIPMENT.md) | 03_EQUIPMENT.md | ✅ Готово | 🔴 Высокий |
| 4 | [Drones (Дроны)](./mechanics/04_DRONES.md) | 04_DRONES.md | ✅ Готово | 🟡 Средний |
| 5 | [Aliens/NPCs (Пришельцы)](./mechanics/05_ALIENS.md) | 05_ALIENS.md | ✅ Готово | 🔴 Высокий |
| 6 | [Resources (Ресурсы)](./mechanics/06_RESOURCES.md) | 06_RESOURCES.md | ✅ Готово | 🔴 Высокий |
| 7 | [Maps & Navigation (Карты)](./mechanics/07_MAPS.md) | 07_MAPS.md | ✅ Готово | 🔴 Высокий |
| 8 | [Factions (Фракции)](./mechanics/08_FACTIONS.md) | 08_FACTIONS.md | ✅ Готово | 🟡 Средний |
| 9 | [Leveling & Rank (Уровни)](./mechanics/09_LEVELING.md) | 09_LEVELING.md | ✅ Готово | 🟡 Средний |
| 10 | [Star Missions (Galaxy Gates)](./mechanics/10_STAR_MISSIONS.md) | 10_STAR_MISSIONS.md | ✅ Готово | 🟡 Средний |
| 11 | Quests (Квесты) | 11_QUESTS.md | ⬜ Не нужно | 🟢 Низкий |
| 12 | Events (События) | 12_EVENTS.md | ⬜ Не нужно | 🟢 Низкий |
| 13 | ~~Economy (Экономика)~~ | — | ❌ Удалено | — |
| 14 | Clans & Social | 14_CLANS.md | ⬜ Не нужно | 🟢 Низкий |
| 15 | Premium Features | 15_PREMIUM.md | ⬜ Не нужно | 🟢 Низкий |
| 16 | [Safe Zones & Repair](./mechanics/16_SAFE_ZONES.md) | 16_SAFE_ZONES.md | ✅ Готово | 🔴 Высокий |
| 17 | PvP System | 17_PVP.md | ⬜ Не нужно | 🟢 Низкий |
| 18 | [Cargo & Collection](./mechanics/18_CARGO.md) | 18_CARGO.md | ✅ Готово | 🔴 Высокий |

**Статистика:** 12/12 нужных механик задокументировано (100%)

---

## Приоритеты для разработки бота

### 🔴 Высокий приоритет (необходимо для MVP) — ВСЕ ГОТОВО ✅

1. **Combat System** — атака NPC, лок цели, kiting, боеприпасы
2. **Ships** — управление кораблём, конфигурации, слоты
3. **Equipment** — понимание урона, скорости, щитов
4. **Aliens/NPCs** — типы мобов, их характеристики, локации
5. **Maps & Navigation** — перемещение между картами, порталы
6. **Resources** — добыча, переработка, улучшение
7. **Safe Zones & Repair** — ремонт на базе, побег
8. **Cargo & Collection** — сбор лута, протокол сбора

### 🟡 Средний приоритет (после MVP) — ВСЕ ГОТОВО ✅

9. **Drones** — увеличение DPS
10. **Factions** — понимание союзников/врагов
11. **Leveling & Rank** — отслеживание прогресса
12. **Star Missions** — дополнительный контент

### 🟢 Низкий приоритет — НЕ НУЖНО для бота

- Quests, Events, Clans, Premium, PvP, Economy

---

## Ключевые открытия для бота

### Боевая система

```
✓ LOCK + ATTACK (однократно) → атака продолжается автоматически
✓ MOVE куда угодно → атака НЕ прерывается
✓ Моб агрится и СЛЕДУЕТ за игроком
✓ Нужно ждать подтверждения LOCK от сервера (PARAM_TARGET_ID=20)
```

### Сбор ресурсов

```
✓ Можно собирать боксы во время боя (радиус 600-900)
✓ Y_OFFSET = 97 — критично для протокола сбора!
✓ MOVE to (box.x, box.y + 97) → ЖДАТЬ → CollectableCollectRequest
✓ MOVE и COLLECT отправляются ПОСЛЕДОВАТЕЛЬНО, не одновременно!
```

### Навигация

```
✓ 3 фракции × 7 карт каждая + Junction карты
✓ Safe Zones:
  - X-1 — ВЕЗДЕ безопасно (вся карта)
  - X-2 до X-6 — у ВСЕХ порталов союзной фракции (если не под атакой)
  - X-7 — у порталов и у базы фракции
✓ Порталы двусторонние — появляешься НА портале другой карты
✓ Исключение: X-1 → X-3 односторонний (обратно попадаешь на X-2)
```

---

## Источники информации

- [WarUniverse Wiki (Fandom)](https://waruniverse.fandom.com/wiki/WarUniverse_Wiki)
- [Официальный сайт](https://waruniverse.space/)
- [Wiki waruniverse.space](https://wiki.waruniverse.space/)
- [Калькулятор урона](https://waruniverse.lol/)
- [Google Play](https://play.google.com/store/apps/details?id=com.spaiowenta.waruniverse)
- [App Store](https://apps.apple.com/us/app/waruniverse/id1441907755)
- [Jetto.net Forum](https://www.jetto.net/forums/war-universe.6/)

---

## Структура файлов

```
D:\Projects\wubot\docs\
├── ARCHITECTURE.md         # Архитектура бота
├── DISCOVERY.md            # Discovery System
├── IMPLEMENTATION_PLAN.md  # План реализации
├── LEGACY_MECHANICS_ANALYSIS.md  # Анализ старого проекта
├── MECHANICS_INDEX.md      # ← Этот файл
├── PACKETS.md              # Протокол пакетов
└── mechanics/
    ├── 01_COMBAT.md        # Боевая система
    ├── 02_SHIPS.md         # Корабли
    ├── 03_EQUIPMENT.md     # Экипировка
    ├── 04_DRONES.md        # Дроны
    ├── 05_ALIENS.md        # NPC/Пришельцы
    ├── 06_RESOURCES.md     # Ресурсы
    ├── 07_MAPS.md          # Карты и навигация
    ├── 08_FACTIONS.md      # Фракции
    ├── 09_LEVELING.md      # Уровни и ранг
    ├── 10_STAR_MISSIONS.md # Galaxy Gates
    ├── 16_SAFE_ZONES.md    # Безопасные зоны
    └── 18_CARGO.md         # Карго и сбор
```

---

## Последнее обновление

Дата: Декабрь 2024
Версия игры: ~1.220+
Документация: v1.1
