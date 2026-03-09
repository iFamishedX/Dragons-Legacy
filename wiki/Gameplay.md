# Gameplay

Dragon's Legacy transforms the Dragon Egg into a persistent, server-wide relic with a single **Bearer** — the player who holds it. This page explains how the Bearer system works, what the Bearer receives, and how the egg changes hands.

---

## Overview

After the Ender Dragon dies and the Dragon Egg is available for the first time, any player can pick it up and become the **Bearer**. The Bearer is a privileged role: they receive ongoing passive bonuses, can activate a powerful ability called **Dragon's Hunger**, and are tracked by every other player on the server. Only one Bearer exists at any given time.

The egg is indestructible — protected from fire, lava, explosions, the void, cactus, and more — so it persists indefinitely until it is deliberately picked up by someone else.

---

## Becoming the Bearer

1. Kill the Ender Dragon to spawn the Dragon Egg.
2. Punch the egg off its pedestal (it teleports to a nearby location as a dropped item).
3. Walk over the dropped egg to pick it up — that player is now the Bearer.

If the current Bearer drops the egg and another player picks it up, the new player immediately becomes the Bearer.

```
Previous Bearer drops the egg
          ↓
Egg enters the world as a dropped item
          ↓
First player to pick it up becomes the new Bearer
```

---

## Bearer Passive Bonuses

While the Bearer holds the Dragon Egg in their main hand or off-hand, they automatically receive:

| Bonus | Default Value |
|---|---|
| **Resistance I** | Reduces all incoming damage by ~20% |
| **Saturation I** | Passively restores food and saturation |
| **+2 Max Hearts** | +4 HP added to the bearer's max health pool |

These bonuses apply instantly when the egg is held and are removed the moment it leaves the bearer's hand. They require no activation. See [Passive Effects](Passive-Effects.md) for full configuration details.

---

## Dragon's Hunger (Active Ability)

In addition to passive bonuses, the Bearer can activate **Dragon's Hunger** — a 5-minute power surge:

```
/dl hunger on
```

| Stat | Default |
|---|---|
| **Duration** | 5 minutes (6000 ticks) |
| **Cooldown** | 1 minute (1200 ticks) after deactivation |
| **Strength II** | Dramatically increases melee damage |
| **Speed II** | Dramatically increases movement speed |
| **+10 Max Hearts** | +20 HP added on top of the passive bonus |
| **+4 Attack Damage** | Flat damage bonus on top of Strength |

The ability can be ended early:

```
/dl hunger off
```

The cooldown begins when the ability ends, whether it expires naturally or is manually deactivated.

> **Note:** Dragon's Hunger blocks Elytra use by default while active. This can be changed in `ability.yaml`.

See [Ability System](Ability-System.md) for scaling options and full configuration.

---

## Egg States

The egg is always in one of five tracked states:

| State | Description |
|---|---|
| `held` | Inside the Bearer's inventory |
| `dropped` | A dropped item entity in the world |
| `placed` | A block placed in the world |
| `entity` | Inside a container entity (chest minecart, etc.) |
| `unknown` | The egg's exact container cannot be determined |

The current state is persisted to disk and survives server restarts. You can check it with:

```
/dl bearer
```

See [Persistence and States](Persistence-and-States.md) for the full technical breakdown.

---

## Tracking the Bearer

All players on the server can check who the Bearer is and, depending on server configuration, where the egg is:

```
/dl bearer
```

**Example output:**
```
[Dragon's Legacy] The current bearer is Steve.
  Egg location: ~120, ~64, ~-340 (Overworld)
  Last seen: 42 seconds ago
```

Admins can configure how precisely the egg's location is revealed for each egg state using **visibility modes**:

| Mode | Behavior |
|---|---|
| `EXACT` | Shows the precise coordinates |
| `RANDOMIZED` | Shows coordinates offset by a random amount |
| `HIDDEN` | Shows no location information |

The default hides the location while the egg is carried by a player (`PLAYER: "HIDDEN"`) but reveals it when dropped or placed. See [Egg Behavior and Protections](Egg-Behavior-and-Protections.md) for details.

---

## Egg Glow

The Dragon Egg always glows in the world to make it visible as a dropped item or placed block. The glow color starts as white (`#FFFFFF`) and can be permanently changed by the bearer by combining the egg with a material in an **anvil**:

| Material | Default Color |
|---|---|
| Amethyst Shard | `#AA00FF` (Purple) |
| Gold Ingot | `#FFD700` (Gold) |
| Redstone | `#FF0000` (Red) |
| Emerald | `#00FF55` (Green) |
| Diamond | `#00FFFF` (Cyan) |
| Iron Ingot | `#D8D8D8` (Silver) |
| Netherite Ingot | `#3C2A23` (Dark brown) |
| Copper Ingot | `#B87333` (Copper) |

See [Glow System](Glow-System.md) for the full list and customization options.

---

## Egg Protections

The Dragon Egg cannot be destroyed by:

- Void (it teleports back to a safe location)
- Fire and lava damage
- Explosions (TNT, creepers, beds in the Nether, etc.)
- Cactus
- Hopper pickup
- Portal teleportation
- Despawn

These protections can each be toggled individually in `egg.yaml`. See [Egg Behavior and Protections](Egg-Behavior-and-Protections.md).

---

## Bearer Reset

The Bearer status can be cleared in two ways:

1. **Offline timeout** — If the Bearer stays offline for more than `offline_reset_days` (default: 3 days), their Bearer status is automatically cleared. The egg remains in place.
2. **Admin reset** — Operators can clear the bearer at any time using the `setbearer` and `clearability` admin subcommands (once implemented).

After a reset, the next player to pick up the egg becomes the new Bearer.

---

## Tips

### For the Bearer

- Keep the egg in your main hand to receive passive bonuses — they stop when you switch items.
- Save Dragon's Hunger for high-stakes fights. The 1-minute cooldown makes repeated activation expensive.
- Use `/dl hunger off` early if you are safe, so the cooldown expires before the next fight.
- Avoid dropping the egg carelessly — any player who picks it up becomes the new Bearer.
- If `block_ender_chest: true` is set, you cannot store the egg in an Ender Chest.

### For Challengers

- Use `/dl bearer` frequently to track the egg's state and location.
- If the egg is in `dropped` or `placed` state with `EXACT` visibility, coordinates are visible — race to pick it up.
- Be ready: the moment you pick up the egg, Dragon's Hunger's cooldown resets and the previous bearer's ability is immediately stripped.

---

## Further Reading

| Page | Description |
|---|---|
| [Ability System](Ability-System.md) | Dragon's Hunger configuration, scaling, and elytra blocking |
| [Passive Effects](Passive-Effects.md) | Configuring the passive bonuses the bearer receives |
| [Egg Behavior and Protections](Egg-Behavior-and-Protections.md) | Visibility modes, protection flags, and container rules |
| [Glow System](Glow-System.md) | Anvil crafting and glow color options |
| [Commands](Commands.md) | Full command reference |
| [Configuration](Configuration.md) | All seven config files explained |
