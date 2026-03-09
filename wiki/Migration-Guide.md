# Migration Guide

This page covers how to migrate your Dragon's Legacy configuration when updating between config layout versions.

---

## Current Layout (v2 — Seven Files)

Since the v2 restructure, Dragon's Legacy uses these configuration files:

```
config/dragonslegacy/
├── global.yaml    — Permissions API, command names, permission nodes
├── egg.yaml       — Egg tracking, visibility, protections
├── ability.yaml   — Dragon's Hunger ability settings
├── passive.yaml   — Always-on passive bonuses
├── infusion.yaml  — Infusion glow colors and materials
├── messages.yaml  — All player-facing text
└── logging.yaml   — Log output categories
```

---

## Migrating from the v1 Layout (Seven Files)

The v1 layout used `config.yaml` + `commands.yaml` + `glow.yaml` instead of the current `global.yaml` + `infusion.yaml`. Follow these steps to migrate:

### Step 1 — Back Up Your Old Configs

```bash
cp config/dragonslegacy/config.yaml config/dragonslegacy/config.yaml.bak
cp config/dragonslegacy/commands.yaml config/dragonslegacy/commands.yaml.bak
cp config/dragonslegacy/glow.yaml config/dragonslegacy/glow.yaml.bak
```

### Step 2 — Stop the Server

Always stop the server before making config changes.

### Step 3 — Delete or Rename the Old Files

```bash
mv config/dragonslegacy/config.yaml config/dragonslegacy/config_old.yaml
mv config/dragonslegacy/commands.yaml config/dragonslegacy/commands_old.yaml
mv config/dragonslegacy/glow.yaml config/dragonslegacy/glow_old.yaml
```

### Step 4 — Start the Server to Generate New Files

Dragon's Legacy detects missing files and generates `global.yaml`, `infusion.yaml`, and `logging.yaml` with default values.

### Step 5 — Transfer Your Settings

| Old File | Old Key | New File | New Key |
|---|---|---|---|
| `config.yaml` | `enabled` | *(removed — mod is always enabled)* | — |
| `commands.yaml` | `root` | `global.yaml` | `commands.root` |
| `commands.yaml` | `aliases` | `global.yaml` | `commands.aliases` |
| `commands.yaml` | `subcommands.help` | `global.yaml` | `commands.help.permission_node` |
| `commands.yaml` | `subcommands.reload` | `global.yaml` | `commands.reload.permission_node` |
| `glow.yaml` | `glow.enabled` | `infusion.yaml` | `infusion.enabled` |
| `glow.yaml` | `glow.color` | `infusion.yaml` | `infusion.default_color` |
| `glow.yaml` | `glow.crafting.materials.<item>` | `infusion.yaml` | `infusion.materials.<item>.color` |
| `egg.yaml` | `offline_reset_days` | *(removed)* | — |
| `egg.yaml` | `block_container_items: false` | `egg.yaml` | `block_container_items: true` (new default) |

### Key Renames in messages.yaml

The following message keys were renamed:

| Old Key | New Key |
|---|---|
| `hunger_activate` | `ability_activated` |
| `hunger_deactivate` | `ability_deactivated` |
| `hunger_expired` | `ability_expired` |
| `announcement_egg_picked_up` | `egg_picked_up` |
| `announcement_egg_dropped` | `egg_dropped` |
| `announcement_egg_placed` | `egg_placed` |
| `announcement_egg_teleported` | `egg_teleported` |
| `announcement_bearer_changed` | `bearer_changed` |
| `announcement_bearer_cleared` | `bearer_cleared` |
| `announcement_ability_activated` | *(merged into `ability_activated`)* |
| `announcement_ability_expired` | *(merged into `ability_expired`)* |
| `announcement_ability_cooldown_started` | `ability_cooldown_started` |
| `announcement_ability_cooldown_ended` | `ability_cooldown_ended` |

A new `disabled: false` field was added to every message entry. Set `disabled: true` to silence any message without removing it.

### ability.yaml / passive.yaml: amplifier → level

The `amplifier` field was renamed to `level` and is now **1-based**:

| Old | New | Effect Level |
|---|---|---|
| `amplifier: 0` | `level: 1` | Level I |
| `amplifier: 1` | `level: 2` | Level II |
| `amplifier: 2` | `level: 3` | Level III |

Update all entries in your `ability.yaml` and `passive.yaml` accordingly.

### ability.yaml: scaling removed

The `scaling` section (`scaling.enabled`, `scaling.health_multiplier`, etc.) has been removed entirely. Delete this section from your `ability.yaml` if it exists.

---

## Reverting to Defaults

To completely reset a config file to defaults:

1. Stop the server.
2. Delete the target config file.
3. Start the server — the file is regenerated with all defaults.

To reset **all** config files:

```bash
rm -r config/dragonslegacy/
```

> **Note:** Resetting config files does **not** reset persistence data. That lives in the world's `data/dragonslegacy/` directory.

---

## Common Migration Mistakes

| Mistake | Symptom | Fix |
|---|---|---|
| Using old `amplifier` field | Effects silently use `level: 1` (default) | Rename to `level` and add 1 |
| Using old message keys (e.g., `hunger_activate`) | Warning in logs, message not found | Rename to new keys (see table above) |
| Keeping `scaling:` in `ability.yaml` | YAML warning about unknown key | Remove the `scaling:` block |
| Editing root/aliases in global.yaml and expecting reload to apply | Commands still use old names | Restart the server fully |
