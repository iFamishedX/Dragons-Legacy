# Dragon's Legacy

**Dragon's Legacy** is a server-side Fabric mod that turns the Dragon Egg into a coveted relic at the
centre of your survival server's meta-game.  The egg changes hands through PvP, exploration, and strategy,
rewarding its bearer with powerful abilities while broadcasting every dramatic moment to the entire server.

---

## Features

- **Egg Core System** – Tracks the Dragon Egg across every state: held in a player's inventory,
  placed as a block, or dropped as an item entity.  Prevents item duplication and handles
  anti-griefing protection automatically.

- **Ability Engine (Dragon's Hunger)** – When a player holds the egg they can activate
  *Dragon's Hunger*, granting powerful combat effects for a configurable duration.  A cooldown
  prevents spamming the ability, and the timer persists across reloads.

- **Announcement System** – Every significant egg event (pick-up, drop, place, bearer change,
  ability activation / expiry, cooldown end) is broadcast server-wide with fully customisable
  colour-code templates.

- **Command System** – A rich `/dragonslegacy` admin command tree lets operators inspect the
  current egg state, force-assign a bearer, clear or reset the ability, and reload the
  configuration at runtime — all without restarting the server.

- **YAML Config System** – All tuneable values (ability duration and cooldown, offline-reset
  threshold, spawn-fallback toggle, announcement templates) are stored in a human-readable
  `config/dragonslegacy/config.yml` that is created automatically on first run and can be
  reloaded live.

- **Offline Bearer Reset** – If the egg bearer stays offline for a configurable number of
  real-world days the mod automatically clears their bearer status so the egg becomes
  contestable again.

- **Spawn Fallback** – When the egg cannot be located anywhere in any dimension the mod
  safely teleports it (or spawns a fresh one) to the world spawn point so it is never
  permanently lost.

- **BlueMap Integration** – Optional BlueMap support places a live marker on your web map
  showing the approximate or exact location of the egg.

- **Placeholder API Support** – Exposes egg and bearer information through the Placeholder API
  for use in other mods or plugins.

---

## Commands

All `/dragonslegacy` sub-commands require the `deg.admin.dragonslegacy` permission (default:
operator level 4).

| Command | Description |
|---|---|
| `/dragonslegacy info` | Display the current egg state, bearer, ability state and timers. |
| `/dragonslegacy setbearer <player>` | Force-assign the Dragon Egg to a player, removing it from its current location. |
| `/dragonslegacy clearability` | Instantly deactivate Dragon's Hunger and clear any cooldown. |
| `/dragonslegacy resetcooldown` | Reset the Dragon's Hunger cooldown so the next bearer may use it immediately. |
| `/dragonslegacy reload` | Reload `config.yml` from disk and apply all changes to running subsystems. |

Additional player-facing commands:

| Command | Description |
|---|---|
| `/dragon_egg bearer` | Show the current bearer's name and the egg's last known location. |
| `/dragon_egg info` | Display server rules and information about the Dragon Egg game. |
| `/dragon_egg help` | List all available commands. |
| `/deg` | Show mod version and links. |

---

## Configuration

The config file is located at:

```
<server root>/config/dragonslegacy/config.yml
```

It is created automatically on first server start.  Edit the file and run `/dragonslegacy reload`
to apply changes without restarting.

### Key options

| Key | Default | Description |
|---|---|---|
| `abilityDurationTicks` | `600` | How long Dragon's Hunger lasts in ticks (600 = 30 s). |
| `abilityCooldownTicks` | `6000` | Cooldown after the ability expires in ticks (6000 = 5 min). |
| `offlineResetDays` | `3.0` | Real-world days a bearer may be offline before the egg is freed. |
| `spawnFallbackEnabled` | `true` | Teleport/spawn the egg at world spawn if it cannot be found. |
| `announcements.*` | (see defaults) | Message templates for every egg and ability event. Supports `§` colour codes and `${placeholder}` tokens. |

### Announcement placeholders

| Key | Available tokens |
|---|---|
| `egg_picked_up` | `${player}` |
| `egg_placed` | `${x}`, `${y}`, `${z}` |
| `egg_teleported_to_spawn` | `${x}`, `${y}`, `${z}` |
| `bearer_changed` | `${player}` |
| `ability_activated` | `${player}`, `${seconds}` |
| `ability_expired` | `${player}` |
| `ability_cooldown_started` | `${seconds}` |

---

## Permissions

| Permission node | Default | Description |
|---|---|---|
| `deg.bearer` | everyone | Use `/dragon_egg bearer`. |
| `deg.info` | everyone | Use `/dragon_egg info`. |
| `deg.help` | everyone | Use `/dragon_egg help`. |
| `deg.admin.info` | op | Use `/deg` (mod info). |
| `deg.admin.reload` | op | Use `/deg reload`. |
| `deg.admin.dragonslegacy` | op | Access all `/dragonslegacy` sub-commands. |
| `deg.admin.dragonslegacy.info` | op | Use `/dragonslegacy info`. |
| `deg.admin.dragonslegacy.setbearer` | op | Use `/dragonslegacy setbearer`. |
| `deg.admin.dragonslegacy.clearability` | op | Use `/dragonslegacy clearability`. |
| `deg.admin.dragonslegacy.resetcooldown` | op | Use `/dragonslegacy resetcooldown`. |
| `deg.admin.dragonslegacy.reload` | op | Use `/dragonslegacy reload`. |

Permissions are managed via [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api),
compatible with LuckPerms and other permission managers.

---

## Supported Versions

| Minecraft | Fabric Loader | Mod version |
|---|---|---|
| 1.21.11 | ≥ 0.18.2 | 1.0.0 |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.11**.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your `mods/` folder.
3. Place `dragonslegacy-1.0.0-1.21.11.jar` into your `mods/` folder.
4. *(Optional)* Install [BlueMap](https://modrinth.com/plugin/bluemap) for the live map integration.
5. Start the server — a default `config/dragonslegacy/config.yml` is created automatically.
6. Customise the config to your liking and run `/dragonslegacy reload`.

> **Server-side only.**  Clients do not need to install this mod.
