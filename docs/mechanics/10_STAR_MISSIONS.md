# 10. Star Missions (Galaxy Gates)

## Обзор

Star Missions (Galaxy Gates) — специальные подземелья с волнами NPC:
- Требуют "параметры" для постройки
- Состоят из нескольких волн
- Дают большие награды
- Есть версии First (удвоенные награды) и Normal

---

## Типы Star Missions

### Protos Invasion Zone

```
Параметров для постройки: 32
Волн: 15
Сложность: ★★★☆☆
Рекомендуемый уровень: 7+
```

**First Protos (первое прохождение):**
```
Награды (примерные):
- PLT: 25,000
- WLX-4: 20,000
- EXP: 4,000,000
- HNR: 100,000
- DCD: 1 (100%)
```

**Волны:**
```
Wave 1: 40 Hydro
Wave 2: 50 Jenta
Wave 3: 60 Mali
Wave 4: 30 HMali, 20 HJenta, 10 HMali
Wave 5: 40 Plarion
Wave 6: 20 Motron
Wave 7: 20 Xeon
Wave 8: 20 HPlarion, 10 HMotron, 3 HXeon
Wave 9: 40 Bangoliour
Wave 10: 15 Zavientos
Wave 11: 15 Magmius
Wave 12: 10 HBango, 3 HZavi, 2 HMagm
Wave 13: 30 Raider
Wave 14: 30 Vortex
Wave 15: Mix Hyper (5 HHydro, 5 HJenta, 5 HMali, 5 HPlarion, 2 HMotron, 1 HXeon, 2 HBano, 1 HZavi, 1 HMagm, 3 HRaider, 3 HVortex)
```

---

### Zeta Aggression Sector

```
Параметров для постройки: 48
Волн: 15
Сложность: ★★★★☆
Рекомендуемый уровень: 10+
```

**First Zeta:**
```
Награды (примерные):
- PLT: 50,000
- WLX-4: 40,000
- EXP: 8,000,000
- HNR: 200,000
- DCD: 1 (100%)
```

**Особенность:** Hyper и Ultra NPC

---

### Eastern Galactic Conflict

```
Параметров для постройки: 74
Волн: 14
Сложность: ★★★★★
Рекомендуемый уровень: 15+
```

**First Eastern:**
```
Награды (примерные):
- PLT: 75,000
- WLX-4: 60,000
- EXP: 12,000,000
- HNR: 300,000
- DCD: 1 (100%)
```

---

### Kratos

```
Параметров для постройки: 96
Волн: 10
Сложность: ★★★★★+
Рекомендуемый уровень: 17+
```

**First Kratos:**
```
Награды (примерные):
- PLT: 150,000
- WLX-4: 50,000
- EXP: 8,000,000
- HNR: 150,000
- BTC: 50,000
- DCD: 1 (100%)
```

**Особенность:** Финальный босс — Kratos (новый тип NPC)

---

### Futurium

```
Параметров для постройки: 120
Волн: 10
Сложность: ★★★★★★
Рекомендуемый уровень: 20+
```

**Награды (примерные):**
```
- PLT: 200,000+
- WLX-4: много
- EXP: 15,000,000+
- HNR: 500,000+
- DCD: 1 (100%) — при первом прохождении
```

---

## Параметры (Gate Parts)

### Получение параметров

```
Источники:
- Убийство NPC (основной)
- Bonus Boxes
- Покупка за PLT
- Events
```

### Типы параметров

```
Каждый тип NPC даёт свой тип параметра:
- Hydro → Hydro Parameter
- Jenta → Jenta Parameter
- Mali → Mali Parameter
- ...и так далее

Также есть Hyper и Ultra параметры.
```

---

## Механика прохождения

### Вход в Gate

```
1. Собрать все параметры
2. Control Panel → Star Missions
3. Выбрать Gate → Build
4. Телепортироваться через специальный портал
```

### Прохождение

```
1. Появляется волна NPC
2. Убить всех NPC
3. Следующая волна появляется автоматически
4. После последней волны — награды
```

### Выход/Смерть

```
При смерти: Возрождение вне Gate
Потеря: Прогресс волн (нужно заново с первой)
Параметры: Не возвращаются
```

---

## First vs Normal

### First (Первое прохождение)

```
NPC: x2 количество (удвоено)
Награды: x2 (удвоены)
DCD: 100% шанс (1 штука)
Доступность: Только один раз
```

### Normal (Повторное)

```
NPC: Стандартное количество
Награды: Стандартные
DCD: Нет
Доступность: Без ограничений
```

---

## Награды DCD

```
Protos → 1 DCD (при первом прохождении)
Zeta → 1 DCD
Eastern → 1 DCD
Kratos → 1 DCD
Futurium → 1 DCD

Максимум DCD: 5 (по одному с каждого First Gate)
```

---

## Стратегия для бота

### Подготовка

```
1. Накопить параметры (фарм NPC)
2. Проверить экипировку (достаточно DPS?)
3. Убедиться в достаточном запасе патронов
```

### Приоритет Gates

```
1. First Protos — относительно легко, хорошие награды
2. First Zeta — средняя сложность
3. Normal Protos — повторный фарм
```

### Расчёт готовности

```java
boolean canDoGate(String gateName) {
    int[] requiredParams = gateRequirements.get(gateName);
    int[] currentParams = inventory.getParams();
    
    for (int i = 0; i < requiredParams.length; i++) {
        if (currentParams[i] < requiredParams[i]) {
            return false;
        }
    }
    
    // Проверка экипировки
    return player.getDPS() > gateMinDPS.get(gateName);
}
```

---

## Связанные механики

- [Aliens](./05_ALIENS.md) — типы NPC в Gates
- [Drones](./04_DRONES.md) — DCD награды
- [Leveling](./09_LEVELING.md) — EXP награды
- [Combat](./01_COMBAT.md) — боевая механика
