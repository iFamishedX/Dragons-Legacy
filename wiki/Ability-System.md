# Ability System

The **Dragon's Hunger** is Dragon's Legacy's central active ability. While the bearer holds the Dragon Egg, they can activate it to receive a dramatic power boost for a configurable duration — after which a cooldown period begins before they can use it again.

---

## Configuration File

```
config/dragonslegacy/ability.yaml
```

Default configuration:

```yaml
config_version: 1

duration_ticks: 6000
cooldown_ticks: 1200
block_elytra: true

effects:
  - id: "strength"
    level: 2
    show_particles: true
    show_icon: true
  - id: "speed"
    level: 2
    show_particles: true
    show_icon: true

attributes:
  - id: "max_health"
    amount: 20.0
    operation: "add_value"
  - id: "attack_damage"
    amount: 4.0
    operation: "add_value"
```

---

## Default Ability Values

| Stat | Value |
|---|---|
| **Duration** | 6000 ticks (5 minutes) |
| **Cooldown** | 1200 ticks (1 minute) |
| **Strength** | Level II |
| **Speed** | Level II |
| **Max Health bonus** | +20 HP (10 hearts) |
| **Attack Damage bonus** | +4 damage |

---

## Activating and Deactivating

### Activation

The bearer activates the ability with:

```
/dl hunger on
```

Activation is blocked if:
1. The command sender is not the current bearer.
2. The ability is already active.
3. The ability is currently on cooldown.

### Deactivation

The ability ends naturally after `duration_ticks` ticks. It can also be ended early:

```
/dl hunger off
```

The cooldown starts from the moment the ability ends (naturally or manually).

---

## Elytra Blocking

When `block_elytra: true`, any attempt to use an Elytra while Dragon's Hunger is active is cancelled and the `elytra_blocked` message is shown.

---

## Effects Configuration

Each entry in the `effects` list applies a vanilla potion effect for the duration of the ability:

```yaml
effects:
  - id: "strength"
    level: 2
    show_particles: true
    show_icon: true
```

| Field | Description |
|---|---|
| `id` | Minecraft effect ID (e.g., `strength`, `speed`); `minecraft:` added if missing |
| `level` | Effect level (1-based: `1` = Level I, `2` = Level II, `3` = Level III, …) |
| `show_particles` | Whether the particle cloud is visible |
| `show_icon` | Whether the effect icon appears in the HUD |

---

## Attributes Configuration

Each entry in the `attributes` list modifies a player attribute while the ability is active:

```yaml
attributes:
  - id: "max_health"
    amount: 20.0
    operation: "add_value"
```

| Field | Description |
|---|---|
| `id` | Minecraft attribute ID; `minecraft:` added if missing |
| `amount` | Value to add or multiply |
| `operation` | `add_value`, `multiply_base`, or `multiply_total` |

---

## Messages

The following messages are sent during ability lifecycle events (configured in `messages.yaml`):

| Message Key | When Sent |
|---|---|
| `ability_activated` | When the ability is activated (title to bearer + chat to everyone) |
| `ability_deactivated` | When the ability is manually deactivated |
| `ability_expired` | When the ability expires naturally (title to bearer + chat to everyone) |
| `ability_cooldown_started` | When the cooldown begins |
| `ability_cooldown_ended` | When the cooldown ends |
| `elytra_blocked` | When the bearer tries to use an Elytra while active |
| `not_bearer` | When a non-bearer tries to use hunger commands |

---

## Related

- [Passive Effects](Passive-Effects.md) — Always-on bonuses that don't require activation
- [Placeholders](Placeholders.md) — `%dragonslegacy:ability_duration%`, `%dragonslegacy:ability_cooldown%`
