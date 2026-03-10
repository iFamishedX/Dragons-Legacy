# Placeholders

Dragon's Legacy provides a **YAML-driven placeholder engine** that exposes configurable external placeholders through PlaceholderAPI, plus a small set of always-registered static placeholders.

All placeholders follow the format:

```
%dragonslegacy:<identifier>%
```

---

## External vs. Internal Placeholders

| Type | Where used | Registered with PAPI? |
|---|---|---|
| **External** (`%dragonslegacy:name%`) | In other plugins, scoreboards, or `messages.yaml` | ✅ Yes |
| **Internal** (`{x}`, `{state}`, `{bearer}`, …) | Inside `placeholders.yaml` format/output strings | ❌ No |

**Internal placeholders** are template variables available *only* inside `placeholders.yaml` format and output strings. They are resolved by the engine at render time but are never registered directly with PlaceholderAPI.

**External placeholders** fall into two groups:
1. **Static** – hardcoded, always registered (player-context info, global prefix)
2. **Config-driven** – defined in `placeholders.yaml`, fully customizable

---

## Static Placeholders

These are always registered and do not require anything in `placeholders.yaml`.

| Placeholder | Description | Example Output |
|---|---|---|
| `%dragonslegacy:global_prefix%` | Value of `prefix` from `messages.yaml` | `[Dragon's Legacy] ` |
| `%dragonslegacy:player%` | Display name of the **event player** (same as executor) | `Steve` |
| `%dragonslegacy:executor%` | Display name of the player who executed the command/event | `AdminPlayer` |
| `%dragonslegacy:executor_uuid%` | UUID of the executing player | `a1b2c3d4-...` |
| `%dragonslegacy:bearer%` | Display name of the current bearer, or `none` | `Alex` |

---

## Config-Driven Placeholders

All other placeholders are defined in `config/dragonslegacy/placeholders.yaml`. The defaults ship with the following entries:

| Placeholder | Description | Respects Visibility? |
|---|---|---|
| `%dragonslegacy:pretty_location%` | Visibility-aware location summary | ✅ Yes |
| `%dragonslegacy:exact-xz%` | Exact X, Z coordinates (admin / debug) | ❌ Always exact |
| `%dragonslegacy:exact-xyz%` | Exact X, Y, Z coordinates (admin / debug) | ❌ Always exact |
| `%dragonslegacy:state%` | Current egg state string | ✅ Yes |
| `%dragonslegacy:bearer_name%` | Name of the bearer, or `None` / `Unknown` | ✅ Yes |
| `%dragonslegacy:dimension%` | Dimension the egg is in | ✅ Yes |
| `%dragonslegacy:ability_status%` | Combined ability state, duration, and cooldown | ❌ Always exact |
| `%dragonslegacy:server_info%` | Online / max players count | ❌ Always exact |

You can add any number of your own entries to `placeholders.yaml`. See the [Configuration](Configuration.md) page for the full file format.

---

## Internal Placeholder Variables

The following internal variables can be used inside `placeholders.yaml` `format:` and `output:` fields:

| Variable | Description |
|---|---|
| `{x}` | Egg X coordinate (exact) |
| `{y}` | Egg Y coordinate (exact) |
| `{z}` | Egg Z coordinate (exact) |
| `{dimension}` | Egg dimension resource key |
| `{state}` | Synthesised state: `PLAYER` / `BLOCK` / `DROPPED` / `UNKNOWN` / `HIDDEN` |
| `{bearer}` | Display name of the current bearer (or `none`) |
| `{bearer_uuid}` | UUID of the current bearer |
| `{executor}` | Name of the player requesting the placeholder |
| `{executor_uuid}` | UUID of that player |
| `{last_seen}` | Time since the bearer was last online (stub, returns `0`) |
| `{seconds}` | Seconds since last seen (stub, returns `0`) |
| `{ability_duration}` | Remaining Dragon's Hunger duration in ticks |
| `{ability_cooldown}` | Remaining Dragon's Hunger cooldown in ticks |
| `{online}` | Current online player count |
| `{max_players}` | Server maximum player count |
| `{world_time}` | Overworld day-time tick |
| `{real_time}` | Server wall-clock time (`HH:mm:ss`) |
| `{tick}` | Alias for `{world_time}` |
| `{egg_age}` | Egg age tracking (stub, returns `0`) |
| `{bearer_health}` | Bearer's current health |
| `{bearer_max_health}` | Bearer's maximum health |

The `{state}` variable is special: when `ignore_visibility: false` and the current visibility mode is `HIDDEN`, `{state}` evaluates to `HIDDEN` so conditions can handle it gracefully.

---

## Supported Filters

Filters can be used inside `{…}` expressions in format/output strings:

| Filter | Description | Example |
|---|---|---|
| `upper(value)` | Uppercase | `{upper({bearer})}` → `ALEX` |
| `lower(value)` | Lowercase | `{lower({state})}` → `player` |
| `capitalize(value)` | First letter upper, rest lower | `{capitalize({state})}` → `Player` |
| `round(value, precision)` | Round to nearest multiple | `{round({x}, 50)}` → `150` |
| `abs(value)` | Absolute value | `{abs({x})}` → `120` |
| `format_number(value)` | Locale-formatted number | `{format_number({online})}` → `1,234` |
| `default(value, fallback)` | Fallback when blank or zero | `{default({bearer}, Nobody)}` |
| `replace(value, target, replacement)` | String replacement | `{replace({state}, PLAYER, Held)}` |
| `if(condition, trueValue, falseValue)` | Inline conditional | `{if({online} > 10, busy, quiet)}` |
| `color(value)` | Translate `§`-codes to `&`-codes | `{color(§aHello)}` |
| `json(val1, val2, …)` | JSON array string | `{json({x}, {y}, {z})}` |
| `distance_to_player()` | Distance from egg to requesting player | `{distance_to_player()}` → `342` |

Filters can be nested: `{round({x}, 50)}`, `{upper({default({bearer}, Nobody)})}`.

---

## Placeholder Definition Structure

Each entry in `placeholders.yaml` follows this structure:

```yaml
my_placeholder:
  ignore_visibility: false   # true = always expose exact values (admin / debug)
  conditions:                # evaluated top-to-bottom; first match wins
    - if: "{state} == 'HIDDEN'"
      output: "Unknown"
    - if: "{state} == 'PLAYER'"
      output: "Carried by {bearer}"
  format: "{round({x},50)}, {round({z},50)}"   # fallback when no condition matches
```

**`ignore_visibility`**
- `false` (default): `{state}` returns `HIDDEN` when the egg's visibility mode is `HIDDEN`
- `true`: `{state}` always returns the real state; intended for admin/debug placeholders

**`conditions`** – evaluated top-to-bottom; the first matching condition determines the output.

**`format`** – the fallback template used when no condition matches.

---

## Visibility Rules

Placeholder visibility is controlled by `egg.yaml`'s `visibility:` map. When `ignore_visibility: false`:

| Visibility Mode | `{state}` value | Recommended handling |
|---|---|---|
| `EXACT` | Real state (`PLAYER`, `BLOCK`, etc.) | Use coordinates directly |
| `RANDOMIZED` | Real state | Use `round({x},50)` etc. in `format:` |
| `HIDDEN` | `HIDDEN` | Add a condition: `if: "{state} == 'HIDDEN'"` |

When `ignore_visibility: true`, `{state}` always returns the real state regardless of the visibility mode.

---

## Checking Values In-Game

Use `/dl placeholders` to print the current value of every registered placeholder for your context.
