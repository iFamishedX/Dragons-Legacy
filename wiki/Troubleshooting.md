# Troubleshooting

This page covers the most common issues encountered when setting up or running Dragon's Legacy, how to diagnose them, and how to fix them.

---

## Diagnostic Commands

### `/dl placeholders`

Prints every placeholder and its current resolved value in your context. This is the first thing to run when something looks wrong.

```
/dl placeholders
```

**Look for:**
- `%dragonslegacy:bearer%` — is the bearer set correctly?
- `%dragonslegacy:egg_state%` — is the egg in the expected state?
- `%dragonslegacy:egg_location%` — is the location being reported as expected?

### `/dl reload`

Reloads all config files from disk. Always run this after editing YAML files (except `global.yaml` command names/aliases — those require a restart).

---

## Common Issues

### The mod is not loading / `/dl` command not found

| Cause | Fix |
|---|---|
| `dragons-legacy-*.jar` not in `mods/` | Copy the jar into the correct folder |
| `fabric-api-*.jar` not in `mods/` | Download and add Fabric API |
| Wrong Minecraft version | Download the jar matching your exact MC version |
| Fabric Loader not installed | Install from [fabricmc.net](https://fabricmc.net/use/) |

---

### Config files were not generated

1. Check the server log for errors during mod initialization.
2. Verify the mod loaded at all (search for `Dragon's Legacy` in the log).
3. Confirm the server has write access to the `config/` directory.

---

### Changes to config files have no effect

- For `messages.yaml`, `egg.yaml`, `ability.yaml`, `passive.yaml`, `infusion.yaml`, `logging.yaml`: run `/dl reload`.
- For `global.yaml` command root/aliases: a **full server restart** is required.
- If the YAML file has a syntax error: the mod keeps the previous version and logs an error. Validate with an online YAML linter.

---

### Passive effects are not applying

1. Run `/dl placeholders` and confirm `%dragonslegacy:bearer%` is your player name.
2. Confirm `%dragonslegacy:egg_state%` is `held`.
3. Check `passive.yaml` — are the effects listed with valid `id` values and 1-based `level`?

Passive effects apply only when the egg is in the bearer's **hand** (main or off-hand).

---

### Dragon's Hunger ability won't activate

| Symptom | Cause | Fix |
|---|---|---|
| `not_bearer` message | Another player is the bearer | The bearer must use the command |
| Cooldown message | Cooldown has not expired | Wait for `cooldown_ticks` ticks |
| Already active message | The ability is running | Use `/dl hunger off` first |

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

If any flag matching the destruction method is `false`, enable it and run `/dl reload`.

---

### Egg location shows empty / wrong coordinates

1. Run `/dl placeholders` and check `%dragonslegacy:x%`, `%dragonslegacy:y%`, `%dragonslegacy:z%`.
2. If empty, visibility mode is `HIDDEN` in `egg.yaml` for the current state.
3. If off by hundreds of blocks, mode is `RANDOMIZED`.
4. Adjust visibility in `egg.yaml` and run `/dl reload`.

---

### MiniMessage tags showing as raw text

Legacy `&` color codes are **not** supported. Use MiniMessage format:

**Wrong:**
```yaml
text: "&aThe bearer is &e%dragonslegacy:bearer%"
```

**Correct:**
```yaml
text: "<green>The bearer is <yellow>%dragonslegacy:bearer%</yellow></green>"
```

---

### Messages not firing

1. Run `/dl placeholders` to verify placeholder values match your conditions.
2. Check `disabled: false` on the message entry.
3. Conditions use exact string matching (case-sensitive).

---

### Validation Warnings in the Log

| Warning | Meaning | Fix |
|---|---|---|
| `Unknown key '...'` | A key is misspelled | Correct the key name |
| `File is version X but current version is Y` | Config needs new keys | Add missing keys per changelog |
| `Failed to parse ...yaml` | YAML syntax error | Validate and fix the file |

---

## Getting More Help

1. Check the [FAQ](FAQ.md) page.
2. Search the [issue tracker](https://github.com/iFamishedX/Dragons-Legacy/issues).
3. File a new issue with reproduction steps, mod version, MC version, and log lines.
