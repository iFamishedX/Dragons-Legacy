# Commands

Dragon's Legacy registers a single root command (`/dragonslegacy`, alias `/dl`) with several subcommands. The root command name, aliases, and per-command permissions are configured in `global.yaml`.

---

## Root Command

| Form | Description |
|---|---|
| `/dragonslegacy` | Full command name |
| `/dl` | Default alias |

The root command without any subcommand shows the help menu (same as `/dl help`).

---

## Subcommands Overview

| Subcommand | Default Name | Description |
|---|---|---|
| `help` | `/dl help` | Show the command help page |
| `bearer` | `/dl bearer` | Display who currently holds the Dragon Egg |
| `hunger on` | `/dl hunger on` | Activate Dragon's Hunger (bearer only) |
| `hunger off` | `/dl hunger off` | Deactivate Dragon's Hunger (bearer or operator) |
| `reload` | `/dl reload` | Reload all config files (op required) |
| `placeholders` | `/dl placeholders` | Print all placeholder values for your current context |

---

## Command Details

### `/dl help`

Displays the help page listing all available commands.

**Usage:**
```
/dl help
```

**Permission:** Configured in `global.yaml` under `commands.help`. Default: no permission required.

---

### `/dl bearer`

Shows the name of the current bearer.

**Usage:**
```
/dl bearer
```

**Permission:** Configured in `global.yaml` under `commands.bearer`. Default: no permission required.

---

### `/dl hunger on`

Activates the Dragon's Hunger ability for the bearer.

**Usage:**
```
/dl hunger on
```

**Permission:** Configured in `global.yaml` under `commands.hunger`. Default: no permission required (bearer-only enforcement is built-in).

**Notes:**
- The ability cannot be activated if:
  - The caller is not the current bearer.
  - The ability is on cooldown.
  - The ability is already active.
- On activation, the configured effects and attribute modifiers are applied for `duration_ticks` ticks.
- If `block_elytra: true` in `ability.yaml`, the bearer cannot use an Elytra while the ability is active.

---

### `/dl hunger off`

Deactivates the Dragon's Hunger ability early.

**Usage:**
```
/dl hunger off
```

**Permission:** Bearer or server operator.

**Notes:**
- All ability effects and attributes are removed immediately.
- The cooldown timer starts from the moment the ability is deactivated.

---

### `/dl reload`

Reloads all seven configuration files from disk without restarting the server.

**Usage:**
```
/dl reload
```

**Permission:** Configured in `global.yaml` under `commands.reload`. Default: op level 3 required.

**Notes:**
- Messages, effects, attributes, infusion colors, and visibility settings all update immediately.
- **Command names and aliases** (`global.yaml`) do **not** update on reload — a full server restart is required for those changes.
- If a config file has a parse error, the mod keeps the previously loaded version and logs an error.

---

### `/dl placeholders`

Prints the current resolved value of every `%dragonslegacy:*%` placeholder for the sender's context.

**Usage:**
```
/dl placeholders
```

**Permission:** Configured in `global.yaml` under `commands.placeholders`. Default: no permission required.

See [Placeholders](Placeholders.md) for descriptions of each value.

---

## Permission Configuration

All public command permissions are configured in `global.yaml`:

```yaml
permissions_api: true

commands:
  help:
    permission_node: "dragonslegacy.command.help"
    op_level: 0

  reload:
    permission_node: "dragonslegacy.command.reload"
    op_level: 3
```

- When `permissions_api: true` → `permission_node` is used (LuckPerms).
- When `permissions_api: false` → `op_level` is used (vanilla).

---

## Permission Summary

| Command | LuckPerms Node | Default Op Level |
|---|---|---|
| `/dl help` | `dragonslegacy.command.help` | 0 |
| `/dl bearer` | `dragonslegacy.command.bearer` | 0 |
| `/dl hunger` | `dragonslegacy.command.hunger` | 0 |
| `/dl reload` | `dragonslegacy.command.reload` | 3 |
| `/dl placeholders` | `dragonslegacy.command.placeholders` | 0 |
