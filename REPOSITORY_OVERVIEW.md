# Dragon's Legacy - Comprehensive Repository Overview

## Table of Contents
1. [Project Summary](#project-summary)
2. [Directory Structure](#directory-structure)
3. [Configuration Files](#configuration-files)
4. [Java Source Code Architecture](#java-source-code-architecture)
5. [Commands](#commands)
6. [Available Placeholders](#available-placeholders)
7. [Key Features](#key-features)
8. [Build Configuration](#build-configuration)
9. [Existing Documentation](#existing-documentation)

---

## Project Summary

**Project Name:** Dragon's Legacy (formerly Dragon Egg Game)
**Version:** 1.0.0
**Minecraft Version:** 1.21.11
**Type:** Fabric Server-Side Mod
**Author:** iFamishedX (originally arvitus/DragonEggGame)
**License:** GPL-3.0
**Repository:** https://github.com/iFamishedX/DragonEggGame

### What It Does

Dragon's Legacy transforms the Dragon Egg into a central, persistent game mechanic for multiplayer SMP servers. It implements a capture-the-flag-like minigame where:

- **The Bearer System:** The player holding the Dragon Egg becomes the "Bearer" and is globally visible
- **Dragon's Hunger:** A powerful 5-minute ability (Strength II, Speed II, +20 max health, +4 attack damage) that only the Bearer can activate
- **Passive Bonuses:** While holding the egg, the bearer gains Resistance I, Saturation I, and +4 max health
- **Complete Egg Protection:** The egg is immune to void, fire, lava, explosions, cactus damage, hopper pickup, portal teleportation, and despawn
- **Persistent State:** The egg's location, bearer, and state are saved to disk and survive server restarts and dimension changes
- **Full Customization:** All messages, command names, effects, attributes, and behaviors are configurable via YAML

---

## Directory Structure

```
Dragons-Legacy/
├── src/main/
│   ├── java/dev/dragonslegacy/                  # 97 Java source files (~7000 LOC)
│   │   ├── DragonsLegacyMod.java                # Main mod entrypoint
│   │   ├── ability/                             # Dragon's Hunger ability system
│   │   │   ├── AbilityEngine.java               # Central ability manager
│   │   │   ├── AbilityState.java
│   │   │   ├── AbilityTimers.java
│   │   │   ├── DragonHungerAbility.java
│   │   │   ├── PassiveEffectsEngine.java        # Passive effect handler
│   │   │   └── event/                           # Ability-related events
│   │   │       ├── AbilityActivatedEvent.java
│   │   │       ├── AbilityDeactivatedEvent.java
│   │   │       ├── AbilityExpiredEvent.java
│   │   │       ├── AbilityCooldownStartedEvent.java
│   │   │       └── AbilityCooldownEndedEvent.java
│   │   ├── egg/                                 # Dragon Egg core system
│   │   │   ├── DragonsLegacy.java               # Main coordinator/singleton
│   │   │   ├── EggTracker.java                  # Tracks egg location/state
│   │   │   ├── EggIdentityManager.java          # Identifies canonical egg
│   │   │   ├── EggPersistentState.java          # Saved state to disk
│   │   │   ├── EggProtectionManager.java        # Protects egg from damage
│   │   │   ├── EggSpawnFallback.java            # Returns egg to spawn
│   │   │   ├── EggAntiDupeEngine.java           # Prevents duplicates
│   │   │   ├── EggOfflineResetManager.java      # Clears bearer after offline days
│   │   │   └── event/                           # Egg-related events
│   │   │       ├── DragonEggEventBus.java       # Event dispatcher
│   │   │       ├── EggEventHandler.java         # Event listeners
│   │   │       ├── EggPickedUpEvent.java
│   │   │       ├── EggDroppedEvent.java
│   │   │       ├── EggPlacedEvent.java
│   │   │       ├── EggBearerChangedEvent.java
│   │   │       └── EggTeleportedToSpawnEvent.java
│   │   ├── config/                              # Configuration system
│   │   │   ├── ConfigManager.java               # Loads all config files
│   │   │   ├── MainConfig.java
│   │   │   ├── CoreConfig.java
│   │   │   ├── EggConfig.java
│   │   │   ├── AbilityConfig.java
│   │   │   ├── PassiveEffectsConfig.java
│   │   │   ├── MessagesConfig.java
│   │   │   ├── CommandsConfig.java
│   │   │   ├── AnnouncementsConfig.java
│   │   │   ├── GlowConfig.java
│   │   │   ├── Data.java                        # Persistent state POJO
│   │   │   ├── MessageString.java               # Message definition
│   │   │   ├── Action.java
│   │   │   ├── Condition.java
│   │   │   ├── AttributeEntry.java
│   │   │   ├── EffectEntry.java
│   │   │   ├── ConfigAttributeParser.java
│   │   │   ├── ConfigEffectParser.java
│   │   │   └── VisibilityType.java
│   │   ├── command/                             # Command registration
│   │   │   ├── DragonsLegacyCommands.java       # Command tree builder
│   │   │   └── MessageOutputSystem.java         # Sends messages to players
│   │   ├── announce/                            # Announcement/message system
│   │   │   ├── AnnouncementManager.java
│   │   │   ├── AnnouncementFormatter.java
│   │   │   └── AnnouncementTemplates.java
│   │   ├── api/                                 # Public API
│   │   │   ├── DragonEggAPI.java                # Core API for other mods
│   │   │   ├── EventsApi.java
│   │   │   ├── APIUtils.java
│   │   │   └── Event.java
│   │   ├── mixin/                               # Fabric mixins (16 classes)
│   │   │   ├── ItemEntityMixin.java             # Track item entity movement
│   │   │   ├── EntityMixin.java
│   │   │   ├── BlockItemMixin.java
│   │   │   ├── InventoryMixin.java              # Track inventory changes
│   │   │   ├── AbstractContainerMenuMixin.java
│   │   │   ├── SlotMixin.java
│   │   │   ├── HopperBlockEntityMixin.java
│   │   │   ├── BaseContainerBlockEntityMixin.java
│   │   │   ├── ContainerEntityMixin.java
│   │   │   ├── CompoundContainerMixin.java
│   │   │   ├── AbstractHorseMixin.java
│   │   │   ├── HorseScreenHandlerMixin.java
│   │   │   ├── ServerLevelMixin.java
│   │   │   └── ...
│   │   ├── interfaces/                          # Custom interfaces for egg tracking
│   │   │   ├── BlockInventory.java
│   │   │   ├── EntityInventory.java
│   │   │   └── DoubleInventoryHelper.java
│   │   ├── utils/                               # Utility classes
│   │   │   ├── Utils.java
│   │   │   ├── CommandNode.java
│   │   │   ├── ScheduledEvent.java
│   │   │   └── ExpressionEvaluator.java
│   │   ├── features/                            # Advanced features
│   │   │   └── Actions.java
│   │   ├── Commands.java                        # Stub (replaced by DragonsLegacyCommands)
│   │   ├── Events.java                          # Event registration
│   │   ├── LootConditions.java                  # Loot table conditions
│   │   ├── MCIntegration.java                   # BlueMap, PlaceholderAPI integration
│   │   ├── Perms.java                           # Permission nodes
│   │   └── Placeholders.java                    # %dragonslegacy:*% placeholders
│   │
│   └── resources/
│       ├── fabric.mod.json                      # Mod metadata
│       ├── dragonslegacy.mixins.json            # Mixin configuration
│       ├── defaults/dragonslegacy/              # Default config files
│       │   ├── config.yaml                      # Global enable/disable
│       │   ├── egg.yaml                         # Egg behavior & protection
│       │   ├── ability.yaml                     # Dragon's Hunger configuration
│       │   ├── passive.yaml                     # Passive effect configuration
│       │   ├── messages.yaml                    # Message templates & channels
│       │   ├── commands.yaml                    # Command names & aliases
│       │   └── glow.yaml                        # Egg glow effect & crafting
│       ├── assets/                              # Textures, models, sounds
│       └── data/                                # Data packs (predicates, loot tables)
│
├── gradle.properties                            # Build configuration
├── build.gradle                                 # Gradle build script
├── README.MD                                    # Main project README
├── Home.md                                      # Wiki index
├── docs/                                        # Additional documentation
├── wiki/                                        # Full wiki pages
│   ├── Home.md                                  # Wiki home
│   ├── Installation.md
│   ├── Configuration.md
│   ├── Messages-and-Prefixes.md
│   ├── Commands.md
│   ├── Placeholders.md
│   ├── Ability-System.md
│   ├── Passive-Effects.md
│   ├── Glow-System.md
│   ├── Egg-Behavior-and-Protections.md
│   ├── Persistence-and-States.md
│   ├── Migration-Guide.md
│   ├── Troubleshooting.md
│   └── FAQ.md
└── .github/                                     # GitHub workflows, CI/CD
```

---

## Configuration Files

All config files are located in `src/main/resources/defaults/dragonslegacy/` and are copied to `config/dragonslegacy/` on first server run.

### 1. config.yaml
**Purpose:** Global mod enable/disable

```yaml
config_version: 1
enabled: true
```

### 2. egg.yaml
**Purpose:** Egg protection, behavior, and visibility settings

**Key Settings:**
- `search_radius: 25` - Radius to search for the egg when loading
- `block_ender_chest: true` - Prevent egg from entering Ender Chests
- `block_container_items: false` - Block egg from specific containers
- `offline_reset_days: 3` - Clear bearer after X days offline
- `nearby_range: 64` - Range for "nearby" predicate

**Visibility Modes:**
- `EXACT` - Show exact coordinates
- `RANDOMIZED` - Show randomized/approximate coordinates
- `HIDDEN` - Don't show location

**Current Visibility Config:**
```yaml
visibility:
  INVENTORY: "EXACT"      # Egg in player inventory
  ITEM: "EXACT"           # Egg dropped on ground
  PLAYER: "HIDDEN"        # Egg held by player
  BLOCK: "RANDOMIZED"     # Egg in container block
  FALLING_BLOCK: "EXACT"
  ENTITY: "EXACT"
```

**Protection Flags:**
```yaml
protection:
  void: true              # Immune to void damage
  fire: true              # Immune to fire
  lava: true              # Immune to lava
  explosions: true        # Immune to explosions
  cactus: true            # Immune to cactus damage
  despawn: true           # Won't despawn if dropped
  hopper: true            # Hoppers can't pick it up
  portal: true            # Won't teleport through portals
```

### 3. ability.yaml
**Purpose:** Dragon's Hunger configuration

**Key Settings:**
```yaml
duration_ticks: 6000     # 5 minutes (300 seconds)
cooldown_ticks: 1200     # 1 minute cooldown
block_elytra: true       # Can't use elytra during ability

scaling:
  enabled: false         # Scale based on player stats
  health_multiplier: 0.0
  damage_multiplier: 0.0
  speed_multiplier: 0.0

effects:                 # Potion effects
  - id: "strength"
    amplifier: 1         # Strength II
    show_particles: true
    show_icon: true
  - id: "speed"
    amplifier: 1         # Speed II

attributes:             # Attribute modifiers
  - id: "max_health"
    amount: 20.0         # +20 max health
    operation: "add_value"
  - id: "attack_damage"
    amount: 4.0          # +4 attack damage
```

### 4. passive.yaml
**Purpose:** Passive effects while holding the egg

```yaml
effects:
  - id: "resistance"     # Resistance I
    amplifier: 0
  - id: "saturation"     # Saturation I
    amplifier: 0

attributes:
  - id: "max_health"
    amount: 4.0          # +4 max health
```

### 5. messages.yaml
**Purpose:** Message templates with conditions, channels, and visibility

**Key Concepts:**
- **Message Key:** Identifier like `help`, `bearer_info`, `bearer_none`
- **Channels:** Multiple output modes (chat, actionbar, title, subtitle, bossbar)
- **Visibility:** bearer_only, everyone, everyone_except_bearer, executor_only, everyone_except_executor
- **Conditions:** Event-based (ability_active, egg_held, egg_dropped, bearer_changed, etc.)
- **Placeholders:** Supports `%dragonslegacy:*%` placeholders
- **Formatting:** Uses MiniMessage format (colors, gradients, hover events, click events)

**Example Message Structure:**
```yaml
help:
  order: 0
  cooldown_ticks: 0                # Per-player cooldown
  global_cooldown_ticks: 0         # Server-wide cooldown
  conditions: {}                   # When to send
  channels:
    - mode: "chat"                 # How to send
      visibility: "everyone"       # Who sees it
      text: |
        %dragonslegacy:global_prefix% Dragon's Legacy Commands
        /dragonslegacy help
        /dragonslegacy bearer
        /dragonslegacy hunger on
```

**Default Messages Included:**
- `help` - Help page
- `bearer_info` - Current bearer info
- `bearer_none` - No bearer yet
- `hunger_activate` - Ability activated
- `hunger_deactivate` - Ability deactivated
- `hunger_expired` - Ability duration ended
- `not_bearer` - Player not bearer error
- `elytra_blocked` - Elytra usage blocked
- `announcement_egg_picked_up` - Egg pickup announcement
- `announcement_egg_dropped` - Egg drop announcement
- `announcement_egg_placed` - Egg placement announcement
- `announcement_bearer_changed` - Bearer changed announcement
- `announcement_bearer_cleared` - Bearer cleared announcement
- `announcement_egg_teleported` - Egg returned to spawn
- `announcement_ability_activated` - Ability activated announcement
- `announcement_ability_expired` - Ability expired announcement
- `announcement_ability_cooldown_started` - Cooldown started
- `announcement_ability_cooldown_ended` - Cooldown ended

### 6. commands.yaml
**Purpose:** Command names and aliases

```yaml
config_version: 1
root: "dragonslegacy"
aliases:
  - "dl"
subcommands:
  help: "help"
  bearer: "bearer"
  hunger: "hunger"
  hunger_on: "on"
  hunger_off: "off"
  reload: "reload"
  test: "test"
  placeholders: "placeholders"
```

### 7. glow.yaml
**Purpose:** Egg glow effects and crafting recipes

```yaml
glow:
  enabled: true
  color: "#FFFFFF"       # Default white glow
  crafting:
    enabled: true
    type: "anvil"        # Combine with anvil
    base_item: "minecraft:dragon_egg"
    materials:           # Material + custom color
      amethyst_shard: "#AA00FF"     # Purple
      copper_ingot: "#B87333"       # Copper
      gold_ingot: "#FFD700"         # Gold
      iron_ingot: "#D8D8D8"         # Gray
      netherite_ingot: "#3C2A23"    # Dark
      quartz: "#E7E7E7"             # White
      redstone: "#FF0000"           # Red
      emerald: "#00FF55"            # Green
      diamond: "#00FFFF"            # Cyan
```

---

## Java Source Code Architecture

### Core Components

#### 1. **DragonsLegacy.java** (Main Coordinator)
- Singleton pattern managing all subsystems
- Initializes on server start via `ServerStartedCallback`
- Coordinates:
  - EggPersistentState (saved data)
  - EggIdentityManager (tracks the one canonical egg)
  - EggTracker (location & state tracking)
  - EggSpawnFallback (returns egg to spawn)
  - EggAntiDupeEngine (prevents duplicates)
  - EggProtectionManager (protects from damage)
  - EggOfflineResetManager (clears bearer after offline)
  - AbilityEngine (Dragon's Hunger)
  - PassiveEffectsEngine (passive bonuses)
  - AnnouncementManager (message sending)

#### 2. **Egg Tracking System**

**EggTracker.java:**
- Tracks egg location via recursive container search
- Searches through:
  - Player inventories
  - Ender chests
  - Chests, barrels, hoppers
  - Shulker boxes, bundles
  - Entity containers (horses, llamas, etc.)
  - Item entities on the ground
- Publishes events when egg state changes
- Respects visibility settings when reporting location

**EggIdentityManager.java:**
- Identifies and marks the canonical Dragon Egg
- Uses custom NBT tag to permanently mark the egg
- Prevents duplication exploits

**EggPersistentState.java:**
- Saves to overworld's DimensionDataStorage
- Persists between server restarts
- Stores:
  - Bearer UUID
  - Egg coordinates
  - Egg state (inventory, item, placed, block, etc.)
  - Last known world dimension
  - Canonical egg ID

#### 3. **Ability System**

**AbilityEngine.java:**
- Manages Dragon's Hunger lifecycle
- States: INACTIVE, ACTIVE, COOLDOWN
- On activation:
  - Applies configured effects (Strength II, Speed II)
  - Applies attribute modifiers (+20 health, +4 damage)
  - Blocks elytra usage if configured
- Publishes events:
  - AbilityActivatedEvent
  - AbilityDeactivatedEvent
  - AbilityExpiredEvent
  - AbilityCooldownStartedEvent
  - AbilityCooldownEndedEvent

**PassiveEffectsEngine.java:**
- Applies passive effects while player holds egg
- Default: Resistance I, Saturation I, +4 health
- Configured in passive.yaml
- Updates every tick if bearer is online

#### 4. **Configuration System**

**ConfigManager.java:**
- Loads all 7 YAML config files
- Uses Configurate library for parsing
- Validates config versions
- Reloadable via `/dl reload` (except command names)

**Config Classes:**
- `MainConfig` - Global settings
- `EggConfig` - Egg behavior & visibility
- `AbilityConfig` - Ability settings
- `PassiveEffectsConfig` - Passive effects
- `MessagesConfig` - Message templates
- `CommandsConfig` - Command names
- `GlowConfig` - Glow system

#### 5. **Event System**

**DragonEggEventBus.java:**
- Central event dispatcher
- Publishes egg and ability events
- Listeners can subscribe for any event type

**Egg Events:**
- `EggPickedUpEvent` - Player picked up egg
- `EggDroppedEvent` - Egg dropped
- `EggPlacedEvent` - Egg placed in block
- `EggBearerChangedEvent` - Bearer changed
- `EggTeleportedToSpawnEvent` - Egg returned to spawn

**Ability Events:**
- `AbilityActivatedEvent`
- `AbilityDeactivatedEvent`
- `AbilityExpiredEvent`
- `AbilityCooldownStartedEvent`
- `AbilityCooldownEndedEvent`

#### 6. **Command System**

**DragonsLegacyCommands.java:**
- Registers `/dragonslegacy` (alias `/dl`) command tree
- All command names are configurable
- Public commands:
  - `help` - Show help
  - `bearer` - Show current bearer
  - `hunger on` - Activate ability
  - `hunger off` - Deactivate ability
- Admin commands:
  - `info` - Full status
  - `setbearer <player>` - Force bearer
  - `clearability` - Clear ability state
  - `resetcooldown` - Reset cooldown
  - `reload` - Reload configs
  - `test <key>` - Test message
  - `placeholders` - List all placeholders

**MessageOutputSystem.java:**
- Renders messages with placeholders
- Supports MiniMessage formatting
- Multiple output channels (chat, actionbar, title, subtitle, bossbar)
- Respects visibility settings per message

#### 7. **Placeholder System**

**Placeholders.java:**
- Registers 17 custom `%dragonslegacy:*%` placeholders
- Integration with PlaceholderAPI

**Available Placeholders:**
- `player` - Executing player name
- `executor` - Same as player
- `executor_uuid` - Executor's UUID
- `bearer` - Current bearer name
- `global_prefix` - Message prefix
- `x`, `y`, `z` - Egg coordinates
- (See full list below)

#### 8. **Mixins** (16 classes)

Mixins allow deep integration with Minecraft:

- **ItemEntityMixin** - Track item entity movement
- **InventoryMixin** - Track inventory changes
- **BlockItemMixin** - Track egg placement
- **AbstractContainerMenuMixin** - Track container changes
- **HopperBlockEntityMixin** - Prevent hopper pickup
- **ServerLevelMixin** - Server-level integration
- **And more** - For tracking egg in all container types

### Code Statistics
- **Total Java Files:** 97
- **Total Lines of Code:** ~7,000
- **Packages:** 15 (ability, egg, config, command, api, mixin, interfaces, utils, announce, features)
- **Event Classes:** 11 (egg & ability events)

---

## Commands

All commands are customizable in `commands.yaml`. These are the defaults:

### Public Commands

#### `/dragonslegacy help` (alias `/dl help`)
- **Description:** Display command help page
- **Permission:** None (all players)
- **Usage:** `/dl help`

#### `/dragonslegacy bearer` (alias `/dl bearer`)
- **Description:** Show current egg bearer and location
- **Permission:** None (all players)
- **Usage:** `/dl bearer`
- **Output includes:** Bearer name, egg location (respecting visibility settings)

#### `/dragonslegacy hunger on` (alias `/dl hunger on`)
- **Description:** Activate Dragon's Hunger ability
- **Permission:** Bearer only
- **Usage:** `/dl hunger on`
- **Activation:** Grants Strength II, Speed II, +20 max health, +4 attack damage for 5 minutes
- **Restrictions:**
  - Only bearer can activate
  - Cannot activate if already active
  - Cannot activate if on cooldown
  - Cannot use elytra while active (if configured)

#### `/dragonslegacy hunger off` (alias `/dl hunger off`)
- **Description:** Deactivate Dragon's Hunger ability early
- **Permission:** Bearer or operator
- **Usage:** `/dl hunger off`
- **Effect:** Immediately removes effects; cooldown begins

### Admin Commands

#### `/dragonslegacy reload` (alias `/dl reload`)
- **Permission:** Operator (OP level 4)
- **Usage:** `/dl reload`
- **Effect:** Reloads all config files (except command names)
- **Note:** Command names require full server restart

#### `/dragonslegacy info` (alias `/dl info`)
- **Permission:** Operator
- **Usage:** `/dl info`
- **Output:** Full status of egg, bearer, ability

#### `/dragonslegacy test <message_key>`
- **Permission:** Operator
- **Usage:** `/dl test help` or `/dl test bearer_info`
- **Effect:** Sends specified message to sender (useful for testing config)

#### `/dragonslegacy placeholders`
- **Permission:** Operator
- **Usage:** `/dl placeholders`
- **Output:** All placeholder values resolved for executor's context

---

## Available Placeholders

All placeholders use the format `%dragonslegacy:placeholder_name%` and are supported in:
- All message text in `messages.yaml`
- Command output
- Any integration with PlaceholderAPI

### Core Placeholders

| Placeholder | Description | Example Output | Permission |
|---|---|---|---|
| `dragonslegacy:player` | Executing player name | `Steve` | None |
| `dragonslegacy:executor` | Same as player | `Steve` | None |
| `dragonslegacy:executor_uuid` | Executor's UUID | `550e8400-e29b-41d4-a716-446655440000` | None |
| `dragonslegacy:bearer` | Current bearer name | `Alex` | None |
| `dragonslegacy:global_prefix` | Configured message prefix | `[Dragon's Legacy]` | None |
| `dragonslegacy:x` | Egg X coordinate | `120` | None |
| `dragonslegacy:y` | Egg Y coordinate | `64` | None |
| `dragonslegacy:z` | Egg Z coordinate | `-340` | None |
| `dragonslegacy:pos` | Formatted position | `~120, ~64, ~-340` | None |
| `dragonslegacy:exact_pos` | Exact position | `120, 64, -340` | deg.placeholders.exact_pos (OP) |
| `dragonslegacy:randomized_pos` | Randomized position | `~118, ~66, ~-342` | deg.placeholders.randomized_pos (OP) |
| `dragonslegacy:item` | Hover text item | (dragon egg icon) | None |

### Additional Placeholders
- `dragonslegacy:egg_state` - Current egg state (inventory, item, placed, etc.)
- `dragonslegacy:egg_location` - Egg's world location
- `dragonslegacy:seconds` - Ability duration remaining
- `dragonslegacy:online` - Number of online players

---

## Key Features

### 1. **Bearer System**
- Only one player at a time
- Cleared after X days offline (configurable)
- Global announcement when bearer changes
- Tracked persistently across restarts

### 2. **Dragon's Hunger Ability**
- Duration: 5 minutes (configurable)
- Cooldown: 1 minute (configurable)
- Grants:
  - Strength II (+4 attack damage)
  - Speed II (doubled movement speed)
  - +20 max health
  - +4 attack damage attribute
- Can be scaled based on player health/damage (optional)
- Blocks elytra usage during active period

### 3. **Passive Effects**
- Active whenever player holds egg
- Default:
  - Resistance I (50% damage reduction)
  - Saturation I (constant food saturation)
  - +4 max health

### 4. **Egg Protection**
The egg is protected from:
- Void damage (teleported to spawn)
- Fire/lava damage
- Explosion damage
- Cactus damage
- Despawning when dropped
- Being picked up by hoppers
- Portal teleportation

### 5. **Egg Tracking**
- Recursive search through all container types
- Searches through:
  - Player inventories
  - Ender chests
  - Chests, barrels, hoppers
  - Shulker boxes, bundles
  - Animal containers (horses, llamas, camels)
  - Item entities on ground
  - Block positions
- Updates in real-time
- Respects visibility settings when reporting

### 6. **Visibility Modes**
- `EXACT` - Show exact coordinates
- `RANDOMIZED` - Show approximate position (±2-3 blocks)
- `HIDDEN` - Don't show location
- Can be configured per egg state (inventory, item, block, etc.)

### 7. **Glow System**
- Combine Dragon Egg with materials in anvil
- 9 material colors included:
  - Amethyst (purple), Copper (orange), Gold (yellow), Iron (gray), Netherite (black), Quartz (white), Redstone (red), Emerald (green), Diamond (cyan)
- Custom hex colors supported
- Glow persists across carries

### 8. **Message System**
- 19+ configurable messages
- Multiple output channels:
  - Chat
  - Action bar
  - Title (above hotbar)
  - Subtitle (below title)
  - Boss bar
- Visibility options:
  - Everyone
  - Bearer only
  - Everyone except bearer
  - Executor only
  - Everyone except executor
- Per-message and global cooldowns
- Conditional display (only show if ability active, etc.)
- MiniMessage formatting (colors, gradients, hover events, click events)

### 9. **Persistent State**
- Saved to overworld's DimensionDataStorage
- Survives:
  - Server restarts
  - Dimension changes
  - Backup/restore cycles
- Auto-recovers if save file is corrupted

### 10. **Full Customization**
- 7 YAML configuration files
- All messages customizable
- All command names customizable
- All effect strengths adjustable
- All cooldowns adjustable
- All protections toggleable
- Reload configs without restart (except commands)

### 11. **API for Other Mods**
- `DragonEggAPI.java` - Query egg state
- `EventsApi.java` - Listen to egg/ability events
- Allows other mods to integrate (e.g., BlueMap markers)

### 12. **Integration**
- **BlueMap:** Customizable egg marker on map
- **PlaceholderAPI:** All 17 placeholders support PlaceholderAPI format
- **LuckPerms:** Uses Fabric Permissions API
- **MiniMessage:** Rich text formatting

---

## Build Configuration

**File:** `gradle.properties`

```properties
minecraft_version=1.21.11
loader_version=0.18.4
mod_id=dragonslegacy
mod_version=1.0.0
maven_group=dev.dragonslegacy

# Dependencies
fabric_version=0.141.3+1.21.11
permissions_api_version=0.6.1
placeholder_api_version=2.8.1+1.21.10
configurate_version=4.2.0
exp4j_version=0.4.8
minimessage_version=4.17.0
```

### Dependencies
- **Minecraft:** 1.21.11
- **Fabric Loader:** 0.18.4+
- **Fabric API:** 0.141.3+
- **Java:** 21+

### Libraries
- **Configurate:** YAML parsing
- **Exp4j:** Expression evaluation
- **MiniMessage:** Text formatting
- **Fabric Permissions API:** Permission checks
- **Placeholder API:** Placeholder support

---

## Existing Documentation

The repository includes a comprehensive wiki with 14 pages covering all aspects:

### Wiki Pages Located in `/wiki/`

1. **Home.md** - Wiki overview, feature list, quick start
2. **Installation.md** - Setup instructions, requirements, dependencies
3. **Configuration.md** - Deep dive into all 7 config files with examples
4. **Messages-and-Prefixes.md** - Message system, channels, visibility, formatting
5. **Commands.md** - All commands, permissions, usage examples
6. **Placeholders.md** - All 17 placeholders with descriptions
7. **Ability-System.md** - Dragon's Hunger deep dive
8. **Passive-Effects.md** - Passive bonuses while holding egg
9. **Glow-System.md** - Egg glow color crafting and customization
10. **Egg-Behavior-and-Protections.md** - All egg protections and states
11. **Persistence-and-States.md** - What's saved to disk, how it persists
12. **Migration-Guide.md** - Upgrading from old single-file config
13. **Troubleshooting.md** - Common issues, diagnostic commands
14. **FAQ.md** - Frequently asked questions

### Additional Documentation
- **README.MD** - Project description and features
- **Home.md** - Quick links to main resources
- **docs/MODRINTH.md** - Modrinth-specific documentation

---

## Summary

Dragon's Legacy is a mature, feature-rich Fabric mod (~7,000 LOC across 97 Java classes) that transforms the Dragon Egg into a central game mechanic. Its architecture is well-organized into focused subsystems (egg tracking, ability management, configuration, commands, events, announcements), with comprehensive YAML configuration files allowing server owners full customization. The mod includes extensive documentation, a public API for other mods, integrations with BlueMap and PlaceholderAPI, and sophisticated egg-tracking through recursive container searches. All messages, effects, commands, and behaviors are configurable without code changes.

