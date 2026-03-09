# Placeholders

Dragon's Legacy provides 17 built-in placeholders that can be used in any `text` value within `messages.yaml`, in condition checks, and in the `/dl placeholders` command.

All placeholders follow the format:

```
%dragonslegacy:<identifier>%
```

---

## Full Placeholder Reference

| Placeholder | Description | Example Output |
|---|---|---|
| `%dragonslegacy:global_prefix%` | The value of `prefix` from `messages.yaml` | `[Dragon's Legacy] ` |
| `%dragonslegacy:player%` | Display name of the **event player** | `Steve` |
| `%dragonslegacy:executor%` | Display name of the player who executed the command | `AdminPlayer` |
| `%dragonslegacy:executor_uuid%` | UUID of the executing player | `a1b2c3d4-...` |
| `%dragonslegacy:bearer%` | Display name of the current bearer, or `none` | `Alex` |
| `%dragonslegacy:x%` | X coordinate of the egg's (possibly randomized) location | `120` |
| `%dragonslegacy:y%` | Y coordinate of the egg's (possibly randomized) location | `64` |
| `%dragonslegacy:z%` | Z coordinate of the egg's (possibly randomized) location | `-340` |
| `%dragonslegacy:dimension%` | Dimension key where the egg currently is | `minecraft:overworld` |
| `%dragonslegacy:egg_location%` | Formatted location string `X, Y, Z (dimension)` | `120, 64, -340 (minecraft:overworld)` |
| `%dragonslegacy:egg_state%` | Current egg state | `held` |
| `%dragonslegacy:last_seen%` | Formatted time since the bearer was last online | `2 hours ago` |
| `%dragonslegacy:seconds%` | Seconds since the bearer was last seen (raw number) | `7320` |
| `%dragonslegacy:ability_duration%` | Remaining ability duration in ticks | `3450` |
| `%dragonslegacy:ability_cooldown%` | Remaining cooldown in ticks | `800` |
| `%dragonslegacy:online%` | Number of players currently online | `12` |
| `%dragonslegacy:max_players%` | Server's maximum player slot count | `20` |

---

## Egg States

`%dragonslegacy:egg_state%` returns one of:

| Value | Meaning |
|---|---|
| `held` | A player is holding the egg |
| `dropped` | The egg is a dropped item entity |
| `placed` | The egg is placed as a block |
| `entity` | The egg is attached to or inside a non-player entity |
| `unknown` | The egg's location cannot be determined |

---

## Coordinate Visibility

`%dragonslegacy:x%`, `%dragonslegacy:y%`, `%dragonslegacy:z%`, and `%dragonslegacy:egg_location%` all respect the `visibility` setting in `egg.yaml` for the current egg state:

- `EXACT` → returns real coordinates
- `RANDOMIZED` → returns offset coordinates
- `HIDDEN` → returns empty string

---

## Using Placeholders in Conditions

Placeholder values can be compared in the `conditions` map of a message:

```yaml
conditions:
  "%dragonslegacy:egg_state%": "held"
  "%dragonslegacy:online%": ">=5"
```

Both conditions must be true for the message to send.

---

## Checking Values In-Game

Use `/dl placeholders` to print the current value of every placeholder for your context.
