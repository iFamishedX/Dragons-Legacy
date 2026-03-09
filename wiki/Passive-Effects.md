# Passive Effects

While the bearer is **holding the Dragon Egg** (in their main hand or off-hand), they continuously receive a set of passive bonuses. Unlike Dragon's Hunger, these bonuses require no activation — they apply automatically and disappear the moment the egg leaves the bearer's hand.

---

## Configuration File

```
config/dragonslegacy/passive.yaml
```

Default configuration:

```yaml
config_version: 1

effects:
  - id: "resistance"
    level: 1
    show_particles: false
    show_icon: false
  - id: "saturation"
    level: 1
    show_particles: false
    show_icon: false

attributes:
  - id: "max_health"
    amount: 4.0
    operation: "add_value"
```

---

## Default Passive Bonuses

| Bonus | Value |
|---|---|
| **Resistance** | Level I — reduces all incoming damage by ~20% |
| **Saturation** | Level I — passively restores food and saturation |
| **Max Health** | +4 HP (2 hearts) |

These values are applied as long as the bearer holds the egg.

---

## Effects List Format

Each entry in `effects` applies a vanilla status effect continuously while the egg is held.

```yaml
effects:
  - id: "resistance"
    level: 1
    show_particles: false
    show_icon: false
```

| Field | Description |
|---|---|
| `id` | Minecraft effect ID (e.g., `resistance`, `saturation`); `minecraft:` added if missing |
| `level` | Effect level (1-based: `1` = Level I, `2` = Level II, …) |
| `show_particles` | Whether the particle cloud is visible |
| `show_icon` | Whether the effect icon appears in the HUD |

Setting `show_particles: false` and `show_icon: false` makes passive bonuses invisible and subtle — the default intentionally.

---

## Attributes List Format

```yaml
attributes:
  - id: "max_health"
    amount: 4.0
    operation: "add_value"
```

| Field | Description |
|---|---|
| `id` | Minecraft attribute ID; `minecraft:` added if missing |
| `amount` | Numeric value to apply |
| `operation` | `add_value`, `multiply_base`, or `multiply_total` |

---

## Adding Custom Passive Effects

Example — add Night Vision and a speed boost:

```yaml
effects:
  - id: "resistance"
    level: 1
    show_particles: false
    show_icon: false
  - id: "saturation"
    level: 1
    show_particles: false
    show_icon: false
  - id: "night_vision"
    level: 1
    show_particles: false
    show_icon: true

attributes:
  - id: "max_health"
    amount: 4.0
    operation: "add_value"
  - id: "movement_speed"
    amount: 0.05
    operation: "add_value"
```

---

## Commonly Used Effect IDs

| ID | Effect |
|---|---|
| `resistance` | Reduces damage taken |
| `saturation` | Restores food / saturation |
| `night_vision` | See in the dark |
| `haste` | Faster mining and attack speed |
| `regeneration` | Restores health over time |
| `fire_resistance` | Immunity to fire and lava |
| `water_breathing` | Breathe underwater |
| `slow_falling` | Reduced fall speed |
| `absorption` | Extra temporary health |

---

## Interaction with Dragon's Hunger

When Dragon's Hunger is active, both passive and ability bonuses apply simultaneously. For example, with the defaults:
- Passive: +4 max HP
- Ability: +20 max HP
- **Total while active: +24 max HP (12 hearts)**
