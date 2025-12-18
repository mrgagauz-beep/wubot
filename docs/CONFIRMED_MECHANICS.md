# Confirmed Game Mechanics

This document contains all game mechanics that have been verified through packet analysis and live testing. Each mechanic includes evidence from actual logs.

> **Note**: Some information in `PACKETS.md` is outdated. This document reflects actual observed behavior.

---

## 1. NPC Parameter IDs (ParamIds)

### Confirmed from packet logs (session_2025-12-12_23-48-43.log)

**NPC Hydro (id: 1000106) - Full Parameter Data:**
```
{id=24, type=0, data=110}      // UI scale (NOT real HP)
{id=25, type=0, data=800}      // REAL HP
{id=26, type=0, data=800}      // REAL MAX_HP
{id=27, type=0, data=560}      // Shield
{id=28, type=0, data=560}      // Max Shield
{id=31, type=0, data=200}      // UI scale (NOT real HP)
{id=34, type=0, data=2000}     // Unknown (NOT HP)
{id=42, type=0, data=3}        // ENTITY_TYPE (3 = NPC)
```

### ParamId Reference Table

| ParamId | Name | Description | Evidence |
|---------|------|-------------|----------|
| **25** | HP | Real current HP (same as players) | Hydro: 800 matches docs "~800-1000 HP" |
| **26** | MAX_HP | Real max HP (same as players) | Hydro: 800 |
| **27** | SHIELD | Current shield value | Observed decreasing during combat |
| **28** | MAX_SHIELD | Maximum shield value | Hydro: 560 |
| **24** | UI_HP_SCALE | UI display value (NOT real HP) | Hydro: 110 (does NOT change during combat) |
| **31** | UI_MAX_HP_SCALE | UI display value (NOT real HP) | Hydro: 200 (does NOT change during combat) |
| **34** | UNKNOWN | Unknown purpose | Hydro: 2000 (constant, NOT HP) |
| **42** | ENTITY_TYPE | Entity classification | 3 = NPC |
| **17** | POSITION | Entity coordinates | float[2] or int[2] |
| **37** | SPEED | Movement speed | float value |

### Important Discovery

**ParamId 24/31 are NOT real HP!** They are UI scale values that do NOT change during combat. The real HP values come from ParamId 25/26, which are the same IDs used for player HP.

**Evidence**: During combat, ParamId 27 (Shield) decreased from 560 to 0, but ParamId 24 remained constant at 110. The NPC died when shield reached 0 and HP (ParamId 25) was depleted.

---

## 2. Attack Protocol

### Lock-Attack Sequence (Confirmed)

1. **LOCK** - Send lock command to target (once)
2. **Wait for confirmation** - Lock is confirmed when `maxHp > 0` appears in response
3. **Approach** - Move within attack range (700px) if too far
4. **ATTACK** - Send attack command (once, server maintains auto-attack)

### Evidence from logs:
```
[COMBAT] Sending LOCK to target: NPC{id=1000136...}
[COMBAT] Lock confirmed! Target HP: 800/800
[COMBAT] Attacking target 1000136 at distance 650px
[SHIELD CHANGE] NPC 1000136 - Shield: 560 -> 332 (delta=-228)
```

### Critical Requirements

- **Attack Range**: 700px maximum
- **Lock Timeout**: 5 seconds (if no confirmation, clear target)
- **HP Visibility**: NPC HP is 0/0 UNTIL locked - this is normal behavior, not an error

### Pre-Lock State

Before lock confirmation, NPCs appear with `hp=0, maxHp=0`. This is expected behavior:
- Do NOT filter out NPCs with hp=0 during target selection
- Do NOT consider NPCs "dead" until `maxHp > 0 && hp <= 0`

---

## 3. Combat Mechanics

### Attack Range and Kiting

| Parameter | Value | Evidence |
|-----------|-------|----------|
| Attack Range | 700px | From COMBAT.md documentation |
| Kite Distance (Weak NPC) | 550px | Must be < attack range |
| Kite Distance (Medium NPC) | 600px | Must be < attack range |
| Kite Distance (Boss NPC) | 650px | Must be < attack range |

**Critical**: Kite distance MUST be less than attack range (700px) or attacks will not deal damage.

### Damage Order

1. **Shield depletes first** - ParamId 27 decreases
2. **HP depletes after shield = 0** - ParamId 25 decreases
3. **NPC dies when HP = 0**

**Evidence from Hydro combat:**
```
Shield: 560 -> 332 -> 91 -> 0
HP: 800 -> (decreases after shield = 0) -> 0 (death)
```

### Damage Confirmation

Damage is confirmed by observing:
- ParamId 27 (Shield) decreasing
- ParamId 25 (HP) decreasing (after shield = 0)
- DAMAGES[] array in packets (contains damage values per tick)

**Warning Threshold**: If no damage for 10+ seconds, attack may not be working (check distance/lock status).

---

## 4. NPC Data (Confirmed)

### Hydro NPC

| Attribute | Value | Source |
|-----------|-------|--------|
| HP | ~800 | Packet logs (ParamId 25 = 800) |
| Max HP | ~800 | Packet logs (ParamId 26 = 800) |
| Shield | 560 | Packet logs (ParamId 27 = 560) |
| Max Shield | 560 | Packet logs (ParamId 28 = 560) |
| npcType | 1 | Code observation |

### NPC Type Identification

| npcType | Name | Notes |
|---------|------|-------|
| 1 | Hydro | Weakest NPC |
| 2 | Hyper | Medium NPC |
| 3+ | Generic/Other | Various NPCs |

**Filter Rule**: Only target entities with `npcType > 0`. Entities with `npcType = 0` are not confirmed NPCs.

**ENTITY_TYPE (ParamId 42)**: Value of 3 confirms entity is an NPC.

---

## 5. Box Collection

### Box Types

| Type | Name | Collection Rule |
|------|------|-----------------|
| 1 | BONUS_BOX | Always collect (valuable resources) |
| 4 | LOOT | Only collect if cargo NOT full |

### Collection Protocol

1. **Move** to box position with Y_OFFSET (+97)
2. **Wait** 700-900ms for arrival
3. **Send** collect request

### Confirmation

Box collection is confirmed by:
- Box entity disappearing from world state
- Cargo value increasing (ParamId 32)

**Note**: This is confirmed by indirect observation (entity removal + cargo change), not a dedicated ACK packet.

---

## 6. Patrol Behavior

### When to Patrol

Patrol activates when:
- No valid NPC targets nearby
- No collectible boxes nearby

### Patrol Algorithm (Bot Implementation)

- Move to random points within map bounds
- Change patrol target every **8 seconds**
- Continue until target/box found

**Note**: 8-second interval is a bot configuration, not a game mechanic.

---

## 7. Position Parsing

### Supported Formats

The POSITION parameter (ParamId 17) can arrive as:
- `int[]` - Integer array [x, y]
- `float[]` - Float array [x, y]
- `double[]` - Double array [x, y]

### Evidence

Earlier bug: Bot "couldn't see" NPCs because position parsing only handled `int[]`. After adding support for all formats, NPCs became visible.

**Implementation**: Safe parsing with try-catch to prevent single bad packet from crashing the tick.

---

## 8. Safe Zone Mechanics

### Safe Zone Sources

Safe zone coordinates come from:
- **Portals** - MapInfoPacket contains portal positions
- **Bases** - Space station coordinates in MapInfoPacket

### Evidence

Earlier bug: Safe zone was hardcoded to (0,0), causing FLEE behavior to fail. After reading coordinates from MapInfoPacket portals/bases, evacuation works correctly.

### Safe Zone Types

- **X-1 Maps** - Entire map is safe zone
- **X-7 Bases** - Base area is safe zone
- **Portal Areas** - Safe if not in combat

---

## 9. Flee Behavior

### Flee Trigger

- HP below threshold (currently 50%)
- Enemy player detected nearby

### Evidence

Earlier bug: `FLEE_HP_PERCENT = 1.01f` (101%) caused bot to ALWAYS flee. After fixing to `0.5f` (50%), bot correctly enters FARMING state.

---

## 10. Discovery System

### Save Triggers

Discovery data saves:
- Every 5 minutes (3000 ticks)
- On map change
- On bot shutdown

### Evidence

Earlier issue: Short sessions resulted in empty `data/` folder because saves only happened every 5 minutes. After adding save-on-map-change and save-on-shutdown, data persists correctly.

---

## 11. Outdated Documentation

### PACKETS.md Discrepancies

The following information in PACKETS.md is **outdated/incorrect**:

| PACKETS.md Says | Reality |
|-----------------|---------|
| ParamId 25/26 = Player HP only | ParamId 25/26 = HP for BOTH NPCs and Players |
| ParamId 31 = SPEED | ParamId 31 = UI scale value (NOT speed) |
| ParamId 37 = unknown | ParamId 37 = SPEED |

**Recommendation**: Use this document (CONFIRMED_MECHANICS.md) as the authoritative reference for ParamIds.

---

## Appendix: Log Evidence

### NPC HP Discovery (from session_2025-12-12_23-48-43.log)

```
NPC Hydro (id: 1000106):
  ParamId 25 = 800  <- REAL HP
  ParamId 26 = 800  <- REAL MAX_HP
  ParamId 24 = 110  <- UI scale (NOT HP)
  ParamId 31 = 200  <- UI scale (NOT HP)
  ParamId 27 = 560  <- Shield
  ParamId 28 = 560  <- Max Shield
```

### Combat Damage Confirmation

```
[SHIELD CHANGE] NPC 1000136 - Shield: 560 -> 332 (delta=-228)
[SHIELD CHANGE] NPC 1000136 - Shield: 332 -> 91 (delta=-241)
[SHIELD CHANGE] NPC 1000136 - Shield: 91 -> 0 (delta=-91)
[HP CHANGE] NPC 1000136 - HP: 800 -> 541 (delta=-259)
...
[HP CHANGE] NPC 1000136 - HP: 59 -> 0 (delta=-59)
```

### Attack Range Issue

```
[COMBAT] Too far to attack (976px > 700px), approaching target...
[COMBAT] Attacking target 1000136 at distance 650px
[SHIELD CHANGE] NPC 1000136 - Shield: 560 -> 332 (delta=-228)
```

---

*Last updated: 2025-12-18*
*Based on packet analysis and live testing*
