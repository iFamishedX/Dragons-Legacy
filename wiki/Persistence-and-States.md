# Persistence and States

Dragon's Legacy saves key information to disk so the bearer's identity, the egg's location, and the mod's state all survive server restarts and crashes. This page explains exactly what is persisted, where it is stored, and how the system behaves at startup.

---

## What Is Persisted

The mod tracks three categories of persistent data:

| Category | What Is Saved |
|---|---|
| **Egg initialization flag** | Whether the egg has been claimed at least once |
| **Bearer identity** | UUID of the current bearer |
| **Last seen tick** | The server tick at which the bearer was last online |

### Egg Initialization (`egg_initialized`)

A boolean flag that is `false` until the Dragon Egg is picked up for the first time. Once set to `true`, the mod begins all tracking behavior. This prevents the system from activating prematurely before the egg has been engaged with.

### Bearer UUID

The UUID (Universally Unique Identifier) of the player who is currently the bearer. Using UUID rather than username means the bearer record survives username changes. The UUID is stored as a standard UUID string:

```
a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

When no bearer is set (egg not yet claimed, or bearer reset), this value is `null` / absent.

### Last Seen Tick

The game tick at which the bearer was most recently online. This value is used to calculate how long the bearer has been offline, which drives:

- The `%dragonslegacy:seconds%` placeholder (seconds since last seen).
- The `%dragonslegacy:last_seen%` placeholder (human-readable duration).
- The **offline reset** check: if `(current_time - last_seen) / 20` seconds exceeds the hardcoded 3-day threshold, the bearer is cleared.

---

## Where Data Is Stored

Persistence data is written to the world's `data/` directory:

```
<world>/data/dragonslegacy/
├── egg_state.dat      (or equivalent NBT/JSON)
```

The exact file format is an implementation detail and should not be edited manually. Always use the mod's commands (`/dl reload`, etc.) to interact with state.

---

## Egg States

The mod tracks five egg states. Each state is persisted alongside the bearer data so the mod knows how to locate the egg on startup.

| State | Description |
|---|---|
| `held` | Egg is in a player's inventory or held in hand |
| `dropped` | Egg is an item entity on the ground |
| `placed` | Egg is placed as a block in the world |
| `entity` | Egg is attached to or inside a non-player entity |
| `unknown` | Egg location is indeterminate |

### State Transitions

```
(not initialized)
      │
      ▼  (first pickup)
    held  ◄──────────────────────────────┐
      │                                   │
      │ (thrown/dropped)                  │ (pickup)
      ▼                                   │
   dropped ──────────────────────────────►│
      │                                   │
      │ (placed as block)                 │
      ▼                                   │
   placed ─────────────────── (broken) ──►│
      │                                   │
      │ (becomes entity, e.g. falling)    │
      ▼                                   │
   entity ─────────────────── (resolves) ─┘
      │
      │ (any unresolvable state)
      ▼
   unknown
```

---

## Ability State Persistence

The **Dragon's Hunger ability state is not persisted** across server restarts. If the server stops while the ability is active:

- The ability ends immediately when the server starts back up.
- The cooldown is **not** carried over — the bearer can use the ability again right away.
- No effects or attribute modifiers from the ability will linger.

This is intentional behavior: ability state is transient and should not be expected to survive restarts.

---

## Offline Reset Logic

The offline reset system monitors bearer absence and clears the bearer status after 3 days of inactivity (hardcoded).

### How It Works

1. When the bearer logs out, the **last seen tick** is recorded.
2. On every server startup (and periodically during runtime), the mod checks how many real-time days have elapsed since `last_seen`.
3. If the elapsed time exceeds the 3-day threshold, the bearer UUID is cleared and `egg_initialized` remains `true`.
4. The next player to pick up the egg becomes the new bearer.

### Disabling Offline Reset

The offline reset threshold is hardcoded at 3 days. To effectively prevent a bearer reset, you can manually clear the persistence file or wait for a future configuration option.

---

## What Happens on First Server Start

1. No persistence data exists yet — `egg_initialized` is `false`.
2. The mod loads and waits.
3. The first player to pick up the Dragon Egg triggers initialization.
4. `egg_initialized` is set to `true`, the bearer UUID is recorded, and the last-seen tick is set.
5. All tracking features become active.

---

## What Happens on Server Restart

1. Persistence data is read from disk.
2. Bearer UUID and last-seen tick are restored.
3. If the bearer is online, effects are **not** automatically re-applied — the bearer must hold the egg again for passive effects to activate.
4. The offline reset check runs immediately on startup.

---

## Manually Resetting State

There is currently no in-game command to manually wipe the bearer. Options available to administrators:

- **Stop the server**, manually edit/delete the persistence file in `<world>/data/dragonslegacy/`, then restart. This is a last resort.
- Stop the server and directly edit or clear the persistence file to reset the bearer.
- Watch for an admin reset command in a future mod update.
