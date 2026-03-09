# Configuration

Dragon's Legacy uses **seven YAML configuration files**, each focused on a specific area of the mod. All files live in:

```
config/dragonslegacy/
├── global.yaml    — Permissions API toggle, command names & aliases, per-command permission nodes
├── egg.yaml       — Egg tracking, visibility, and protections
├── ability.yaml   — Dragon's Hunger ability settings
├── passive.yaml   — Passive effects while holding the egg
├── infusion.yaml  — Infusion color and anvil material settings
├── messages.yaml  — All player-facing text
└── logging.yaml   — Log output categories
```

After editing any file, run `/dl reload` to apply changes without restarting the server. Fields marked **[Restart]** require a full server restart to take effect.

---

## global.yaml

Controls the permission system toggle, root command name, aliases, and per-command permission nodes/op levels.

```yaml
config_version: 1

# If true → use LuckPerms permission nodes.
# If false → use vanilla operator levels.
permissions_api: true

commands:
  root: "dragonslegacy"   # [Restart]
  aliases:                # [Restart]
    - "dl"

  help:
    permission_node: "dragonslegacy.command.help"
    op_level: 0

  bearer:
    permission_node: "dragonslegacy.command.bearer"
    op_level: 0

  hunger:
    permission_node: "dragonslegacy.command.hunger"
    op_level: 0

  reload:
    permission_node: "dragonslegacy.command.reload"
    op_level: 3

  placeholders:
    permission_node: "dragonslegacy.command.placeholders"
    op_level: 0
```

| Key | Type | Default | Description |
|---|---|---|---|
| `permissions_api` | boolean | `true` | When `true`, uses LuckPerms nodes; when `false`, uses vanilla op levels |
| `commands.root` | string | `"dragonslegacy"` | Primary command name **[Restart]** |
| `commands.aliases` | list | `["dl"]` | Alternate command names **[Restart]** |
| `commands.*.permission_node` | string | *(see above)* | LuckPerms node (used when `permissions_api: true`) |
| `commands.*.op_level` | integer | *(see above)* | Vanilla op level (used when `permissions_api: false`) |

---

## egg.yaml

Controls how the egg is tracked, where it can appear in the world, and what protections it has.

```yaml
config_version: 1

# search_radius: [Restart]
# nearby_range: (reload)

search_radius: 25
nearby_range: 64

block_ender_chest: true
block_container_items: true

visibility:
  INVENTORY: EXACT
  ITEM: EXACT
  PLAYER: HIDDEN
  BLOCK: RANDOMIZED
  FALLING_BLOCK: EXACT
  ENTITY: EXACT

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

### Top-Level Keys

| Key | Type | Default | Description |
|---|---|---|---|
| `search_radius` | integer | `25` | Block radius searched when locating a nearby egg **[Restart]** |
| `nearby_range` | integer | `64` | Block radius for "nearby" egg detection (commands/placeholders) |
| `block_ender_chest` | boolean | `true` | Prevents the Dragon Egg from entering an Ender Chest |
| `block_container_items` | boolean | `true` | Prevents the Dragon Egg from entering any container |

### Egg States

The egg can be in one of the following states:

| State | Description |
|---|---|
| `HELD` | Egg is in a player's inventory |
| `DROPPED` | Egg is an item entity on the ground |
| `PLACED` | Egg is placed as a block |
| `ENTITY` | Egg is attached to or riding an entity |
| `UNKNOWN` | State cannot be determined |

### Visibility Modes

Each egg state context can independently control how much location information is revealed.

| Mode | Behavior |
|---|---|
| `EXACT` | Reports the real coordinates |
| `RANDOMIZED` | Offsets the reported coordinates by a random amount |
| `HIDDEN` | Returns no location information |

| Context Key | When it Applies |
|---|---|
| `INVENTORY` | Egg is in a player's inventory |
| `ITEM` | Egg is a dropped item entity |
| `PLAYER` | Egg is being held by a player |
| `BLOCK` | Egg is placed as a block |
| `FALLING_BLOCK` | Egg is a falling block entity |
| `ENTITY` | Egg is riding or attached to an entity |

### Protection Flags

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

## ability.yaml

Configures the **Dragon's Hunger** ability — the active ability the bearer can trigger.

```yaml
config_version: 1

duration_ticks: 6000
cooldown_ticks: 1200
block_elytra: true

effects:
  - id: "strength"
    level: 2
    show_particles: true
    show_icon: true
  - id: "speed"
    level: 2
    show_particles: true
    show_icon: true

attributes:
  - id: "max_health"
    amount: 20.0
    operation: "add_value"
  - id: "attack_damage"
    amount: 4.0
    operation: "add_value"
```

| Key | Type | Default | Description |
|---|---|---|---|
| `duration_ticks` | integer | `6000` | How long the ability lasts (6000 = 5 minutes) |
| `cooldown_ticks` | integer | `1200` | Time between uses (1200 = 1 minute) |
| `block_elytra` | boolean | `true` | Prevents the bearer from using an Elytra while active |

### Effects List

Each entry in `effects` applies a vanilla potion effect.

| Field | Description |
|---|---|
| `id` | Minecraft effect ID (e.g., `strength`, `speed`); namespace auto-completed to `minecraft:` |
| `level` | Effect level (1-based: `1` = Level I, `2` = Level II, …) |
| `show_particles` | Whether the particle cloud is visible around the player |
| `show_icon` | Whether the effect icon shows in the HUD |

### Attributes List

Each entry in `attributes` modifies a player attribute.

| Field | Description |
|---|---|
| `id` | Minecraft attribute ID (e.g., `max_health`, `attack_damage`); namespace auto-completed |
| `amount` | Numeric value to add or multiply |
| `operation` | `add_value`, `multiply_base`, or `multiply_total` |

---

## passive.yaml

Configures the always-on bonuses the bearer receives just from holding the egg.

```yaml
config_version: 1

effects:
  - id: "resistance"
    level: 1
    show_particles: false
    show_icon: false
  - id: "saturation"
    level: 1
    show_particles: false
    show_icon: false

attributes:
  - id: "max_health"
    amount: 4.0
    operation: "add_value"
```

The `effects` and `attributes` lists follow the same format as in `ability.yaml` (see above). Effects are reapplied every tick the bearer is holding the egg, and attributes are added/removed as the bearer picks up or puts down the egg.

See [Passive Effects](Passive-Effects.md) for a detailed explanation.

---

## infusion.yaml

Controls the egg's glow color and the anvil-based infusion system for changing it.

```yaml
config_version: 1

infusion:
  enabled: true
  default_color: "#FFFFFF"

  materials:
    amethyst_shard:
      color: "#AA00FF"
      tooltip: "Infused with Amethyst"

    copper_ingot:
      color: "#B87333"
      tooltip: "Infused with Copper"

    gold_ingot:
      color: "#FFD700"
      tooltip: "Infused with Gold"
    # ... (see full list in the file)
```

| Key | Type | Default | Description |
|---|---|---|---|
| `infusion.enabled` | boolean | `true` | Enables the infusion system |
| `infusion.default_color` | hex string | `"#FFFFFF"` | Default glow color before any infusion |
| `infusion.materials` | map | *(see above)* | Map of material keys to `{color, tooltip}` entries |

**Notes:**
- The Dragon Egg is always the base item (hardcoded; not configurable).
- Item namespace is auto-completed (`minecraft:` added if missing).
- `tooltip` is rendered in the infusion color.

See [Infusion System](Glow-System.md) for the full materials reference.

---

## messages.yaml

Contains every player-facing text string. See [Messages and Prefixes](Messages-and-Prefixes.md) for the full format documentation.

```yaml
config_version: 1
prefix: ""

messages:
  help:
    disabled: false
    order: 0
    cooldown_ticks: 0
    global_cooldown_ticks: 0
    conditions: {}
    channels:
      - mode: chat
        visibility: everyone
        text: "<gray>Dragon's Legacy help..."
```

Key concepts at a glance:

- All text uses **MiniMessage** format — `<red>`, `<gradient:...>`, `<hover:...>`, etc.
- Each message has one or more **channels** with a `mode` (`chat`, `actionbar`, `title`, `subtitle`, `bossbar`) and a `visibility` (`bearer_only`, `everyone`, `everyone_except_bearer`, `executor_only`, `everyone_except_executor`).
- `disabled: true` silences a message entirely without removing it from the file.
- `conditions` allows messages to be gated behind state checks.
- `cooldown_ticks` / `global_cooldown_ticks` throttle how often a message fires per-player or server-wide.

---

## logging.yaml

Controls which categories of log output appear in the server console and are sent to online operators.

```yaml
config_version: 1

logging:
  enabled: true

  messages:
    console: true
    ops: true

  config_validation:
    console: true
    ops: true

  state_changes:
    console: true
    ops: true

  egg_events:
    console: true
    ops: true

  ability_events:
    console: true
    ops: true

  errors:
    console: true
    ops: true
```

| Category | Description |
|---|---|
| `messages` | General mod messages |
| `config_validation` | Warnings about unknown/invalid config values |
| `state_changes` | Egg state transitions (picked up, placed, dropped, etc.) |
| `egg_events` | Egg protection and tracking events |
| `ability_events` | Dragon's Hunger activation, expiry, and cooldown events |
| `errors` | Runtime errors and exceptions |

Set `logging.enabled: false` to silence all Dragon's Legacy log output.
