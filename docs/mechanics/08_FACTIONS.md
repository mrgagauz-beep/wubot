# 08. Factions (Фракции)

## Обзор

В WarUniverse существуют 3 фракции:
- **Orion Empire (E)** — синие
- **Vega Union (U)** — зелёные  
- **Solar Conglomerate (R)** — красные

Фракция выбирается при создании персонажа и определяет:
- Стартовую базу
- Союзников и врагов
- Доступ к территориям

---

## Фракции

### Orion Empire (E)

```
Цвет: Синий
Карты: E-1 → E-7
База: E-1, E-7
Союзники: Orion Empire
Враги: Vega Union, Solar Conglomerate
```

### Vega Union (U)

```
Цвет: Зелёный
Карты: U-1 → U-7
База: U-1, U-7
Союзники: Vega Union
Враги: Orion Empire, Solar Conglomerate
```

### Solar Conglomerate (R)

```
Цвет: Красный
Карты: R-1 → R-7
База: R-1, R-7
Союзники: Solar Conglomerate
Враги: Orion Empire, Vega Union
```

---

## Территории

### Домашние карты (Safe)

```
X-1: Начальная база — полностью безопасна
X-2: Фарм зона — относительно безопасна
X-3: Фарм зона — относительно безопасна
```

### Пограничные карты (Danger)

```
X-4 (Junction): Зона конфликта — высокий риск PvP
X-5: Зона конфликта — риск PvP
```

### Дальние карты

```
X-6: Сложная зона — сильные NPC + PvP
X-7: Вторая база — Safe Zone у базы
```

---

## Взаимоотношения фракций

### На своей территории

```
Союзники (своя фракция):
- Нельзя атаковать
- Можно помогать
- Безопасность

Враги (другие фракции):
- На X-1, X-2, X-3: Редко появляются
- На X-4+: Могут атаковать
```

### На вражеской территории

```
Вы — враг для местных:
- Могут атаковать без предупреждения
- Нет Safe Zone (кроме своих баз)
- Высокий риск
```

### На Junction картах

```
J-VO, J-VS, J-SO:
- Нейтральная зона
- Все фракции — враги друг другу
- Наивысший риск PvP
```

---

## Смена фракции

```
Возможность: Да, ограниченно
Место: Settings → Account
Ограничение: Раз в определённый период
Последствия:
- Новая стартовая точка
- Потеря позиции в клане (если был)
- Бывшие союзники становятся врагами
```

---

## Определение фракции игроков

### По ID карты

```java
int getMapFaction(int mapId) {
    String mapName = mapRegistry.getName(mapId);
    
    if (mapName.startsWith("E-") || mapName.equals("J-VO") || mapName.equals("J-SO")) {
        return ORION_EMPIRE;
    }
    if (mapName.startsWith("U-") || mapName.equals("J-VS") || mapName.equals("J-VO")) {
        return VEGA_UNION;
    }
    if (mapName.startsWith("R-") || mapName.equals("J-VS") || mapName.equals("J-SO")) {
        return SOLAR_CONGLOMERATE;
    }
    return NEUTRAL;
}
```

### По кораблю игрока

```java
// В GameStateResponsePacket
// Ship.faction или определение по spawn point
```

---

## Влияние на бота

### Безопасный фарм

```
Рекомендация: Оставаться на X-1, X-2, X-3 своей фракции
Риск PvP: Минимальный
```

### Рискованный фарм

```
Карты: X-4, X-5, X-6
Риск: Средний-Высокий
Действия: Быть готовым к побегу
```

### Избегание врагов

```java
boolean isEnemy(Ship ship) {
    if (ship.npcId > 0) return true;  // NPC — всегда враг
    
    int playerFaction = getMyFaction();
    int shipFaction = ship.getFaction();
    
    return playerFaction != shipFaction;
}

void checkThreats(WorldSnapshot world) {
    for (Ship ship : world.players()) {
        if (isEnemy(ship) && distance(player, ship) < THREAT_RANGE) {
            flee();
            return;
        }
    }
}
```

---

## События фракций

### Spaceball

```
Тип: PvP командное событие
Место: T-1, G-1
Цель: Забить мяч в ворота врагов
Награда: +1 час бонуса к наградам для победившей фракции
```

### Group Mission

```
Тип: PvE командное событие
Место: X-5 карты всех фракций
Цель: Защитить территорию от волн NPC
Награда: Распределяется по урону
```

### Convoy

```
Тип: PvP событие
Цель: Защитить/атаковать конвой
```

---

## Статистика фракций

В игре можно посмотреть:
- Общий счёт фракции
- Топ игроков фракции
- История событий

---

## Связанные механики

- [Maps](./07_MAPS.md) — территории фракций
- [PvP](./17_PVP.md) — взаимодействие с врагами
- [Events](./12_EVENTS.md) — события фракций
- [Clans](./14_CLANS.md) — объединения внутри фракций
