# Egg Behavior and Protections

Dragon's Legacy tracks the Dragon Egg at all times and applies several protection layers to prevent loss, exploitation, or accidental destruction.

---

## Configuration File

```
config/dragonslegacy/egg.yaml
```

---

## Egg States

The egg can be in one of five states at any given time:

| State | Description |
|---|---|
| `HELD` | Egg is in a player's inventory |
| `DROPPED` | Egg is a dropped item entity on the ground |
| `PLACED` | Egg is placed as a block in the world |
| `ENTITY` | Egg is attached to or riding another entity |
| `UNKNOWN` | State cannot be determined |

The egg's state determines which visibility mode is used for coordinate reporting (see `egg.yaml` → `visibility`).

---

## Container Blocking

```yaml
block_ender_chest: true
block_container_items: true
```

| Setting | Effect |
|---|---|
| `block_ender_chest` | Prevents the Dragon Egg from being placed into an Ender Chest |
| `block_container_items` | Prevents the Dragon Egg from being placed into any container |

These settings only apply to the Dragon Egg, not to other items.

---

## Visibility Modes

Each egg state has a configurable visibility mode that controls how much coordinate information is revealed by commands and placeholders:

```yaml
visibility:
  INVENTORY: EXACT
  ITEM: EXACT
  PLAYER: HIDDEN
  BLOCK: RANDOMIZED
  FALLING_BLOCK: EXACT
  ENTITY: EXACT
```

| Mode | Behavior |
|---|---|
| `EXACT` | Reports the real coordinates |
| `RANDOMIZED` | Offsets the reported coordinates by a random amount |
| `HIDDEN` | Returns no location information |

The `search_radius` setting (**[Restart]** required) controls the search radius used when locating a lost egg.

---

## Protection Flags

All protections are enabled by default. Disable any by setting it to `false`.

```yaml
protection:
  void: true
  fire: true
  lava: true
  explosions: true
  cactus: true
  despawn: true
  hopper: true
  portal: true
```

| Flag | What It Prevents |
|---|---|
| `void` | Egg is teleported back when falling into the void |
| `fire` | Egg item entity is immune to fire damage |
| `lava` | Egg item entity is immune to lava |
| `explosions` | Egg item entity and placed block survive explosions |
| `cactus` | Egg item entity is immune to cactus damage |
| `despawn` | Egg item entity never despawns naturally |
| `hopper` | Hoppers cannot pick up the egg item |
| `portal` | Egg does not travel through Nether/End portals |

---

## Nearby Range

```yaml
nearby_range: 64
```

Controls how close a player must be to count as "nearby" the egg for tracking, announcements, and the `is_nearby` predicate.

---

## Persistence

The egg's state (location, bearer, ability timers) is saved to persistent world data and survives server restarts. See [Persistence and States](Persistence-and-States.md) for details.
