# Troubleshooting

This page covers the most common issues encountered when setting up or running Dragon's Legacy, how to diagnose them, and how to fix them.

---

## Diagnostic Commands

Before diving into specific issues, use these commands to quickly understand the current state of the mod:

### `/dl placeholders`

Prints every placeholder and its current resolved value in your context. This is the first thing to run when something looks wrong.

```
/dl placeholders
```

**Look for:**
- `%dragonslegacy:bearer%` — is the bearer set correctly?
- `%dragonslegacy:egg_state%` — is the egg in the expected state?
- `%dragonslegacy:egg_location%` — is the location being reported as expected?

### `/dl test <key>`

Renders and sends a specific message to you directly, bypassing visibility and cooldown rules. Use this to verify that a message looks correct after editing `messages.yaml`.

```
/dl test ability_start
/dl test bearer_changed
/dl test help
```

If the message renders correctly with `/dl test` but does not fire in-game, the issue is likely a **condition** or **cooldown** in the message definition.

### `/dl reload`

Reloads all config files from disk. Always run this after editing YAML files (except `commands.yaml` — that requires a restart).

---

## Common Issues

### The mod is not loading / `/dl` command not found

**Possible causes:**

| Cause | Fix |
|---|---|
| `dragons-legacy-*.jar` not in `mods/` | Copy the jar into the correct folder |
| `fabric-api-*.jar` not in `mods/` | Download and add Fabric API |
| Wrong Minecraft version | Download the jar matching your exact MC version |
| Fabric Loader not installed | Install Fabric Loader from [fabricmc.net](https://fabricmc.net/use/) |

Check your server startup log for errors from Dragon's Legacy or Fabric.

---

### Config files were not generated

The mod generates config files on first startup. If they are missing:

1. Check the server log for errors during mod initialization.
2. Verify the mod loaded at all (search for `Dragon's Legacy` in the log).
3. Confirm the server has write access to the `config/` directory.
4. If using a Docker container or managed host, check for read-only filesystem restrictions.

---

### Changes to config files have no effect

**If you edited `messages.yaml`, `egg.yaml`, `ability.yaml`, `passive.yaml`, or `glow.yaml`:**
- Run `/dl reload`. Changes to these files do not require a restart.

**If you edited `commands.yaml` (root name or aliases):**
- A **full server restart** is required. Fabric registers commands at startup; `/dl reload` cannot re-register them.

**If the YAML file has a syntax error:**
- The mod keeps the previously loaded version and logs an error.
- Check the server log for lines like `[Dragon's Legacy] [ERROR] Failed to parse egg.yaml`.
- Validate your YAML with an online linter (e.g., [yamllint.com](https://www.yamllint.com/)) and fix indentation errors.

---

### Passive effects are not applying

1. Run `/dl placeholders` and confirm `%dragonslegacy:bearer%` is your player name.
2. Confirm `%dragonslegacy:egg_state%` is `held`.
3. Check `passive.yaml` — are the effects listed correctly with valid `id` values?
4. Check `config.yaml` — is `enabled: true`?

Passive effects apply only when the egg is in the bearer's **hand** (main or off-hand), not when it is in their inventory.

---

### Dragon's Hunger ability won't activate

| Symptom | Cause | Fix |
|---|---|---|
| "You are not the bearer" message | Another player is the bearer | The current bearer must use the command |
| "Ability on cooldown" | Cooldown has not expired | Wait for `cooldown_ticks` ticks |
| "Ability already active" | The ability is currently running | Use `/dl hunger off` first if needed |
| No response at all | `enabled: false` in `config.yaml` | Set `enabled: true` and reload |

---

### Egg keeps getting destroyed

Check which protection flags are enabled in `egg.yaml`:

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

If any flag that matches the destruction method is `false`, enable it and run `/dl reload`.

If the egg is being destroyed by a method not listed here, please file a bug report with exact reproduction steps.

---

### Egg location shows empty / wrong coordinates

1. Run `/dl placeholders` and check `%dragonslegacy:egg_state%` and `%dragonslegacy:x%`, `%dragonslegacy:y%`, `%dragonslegacy:z%`.
2. If `x`, `y`, `z` are empty, the visibility mode for the current egg state is set to `HIDDEN` in `egg.yaml`.
3. If the coordinates look wrong (off by hundreds of blocks), the mode is `RANDOMIZED`.
4. Adjust the visibility settings in `egg.yaml` and run `/dl reload`.

---

### MiniMessage tags showing as raw text

All text must use MiniMessage format. Legacy `&` color codes are **not** supported.

**Wrong:**
```yaml
text: "&aThe bearer is &e%dragonslegacy:bearer%"
```

**Correct:**
```yaml
text: "<green>The bearer is <yellow>%dragonslegacy:bearer%</yellow></green>"
```

Use the [MiniMessage viewer](https://webui.advntr.dev/) to preview your formatting before deploying.

---

### Messages not firing / conditions not matching

1. Run `/dl test <key>` — if the message shows up here, the message definition is correct.
2. Run `/dl placeholders` — check the exact value of the placeholder used in your condition.
3. Conditions use exact string matching by default. If `%dragonslegacy:egg_state%` returns `"held"` but your condition says `"Held"` (capital H), it will not match.
4. Numeric comparisons (`>=`, `<=`, `>`, `<`) require the placeholder to return a plain number string.

---

### Validation Warnings in the Log

```
[Dragon's Legacy] [WARN] Unknown key 'effets' in passive.yaml — did you mean 'effects'?
[Dragon's Legacy] [WARN] egg.yaml is version 1 but current version is 2.
```

| Warning | Meaning | Fix |
|---|---|---|
| `Unknown key '...'` | A key in your config is misspelled or doesn't exist | Correct the key name |
| `File is version X but current version is Y` | Config file needs new keys | Add the missing keys (see changelog) and update `config_version` |
| `Failed to parse ...yaml` | YAML syntax error | Validate the file and fix indentation/quoting |

---

## Getting More Help

If your issue is not covered here:

1. Check the [FAQ](FAQ.md) page.
2. Search the [issue tracker](https://github.com/iFamishedX/DragonEggGame/issues) for similar reports.
3. File a new issue on [GitHub](https://github.com/iFamishedX/DragonEggGame/issues/new) with the reproduction steps, mod version, Minecraft version, and relevant log lines.
