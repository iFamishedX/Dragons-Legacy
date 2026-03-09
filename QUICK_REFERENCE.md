# Dragon's Legacy - Quick Reference Guide

## Project At A Glance

| Attribute | Value |
|-----------|-------|
| **Type** | Fabric Server-Side Mod |
| **Version** | 1.0.0 |
| **Minecraft** | 1.21.11 |
| **Java** | 21+ |
| **License** | GPL-3.0 |
| **Code** | ~7,000 LOC across 97 files |

## What It Does (In One Sentence)
Transforms the Dragon Egg into a persistent, tracked game item granting the holder (Bearer) special abilities and making them the target of a capture-the-flag minigame.

## Core Mechanics

### Bearer System
- One player at a time holds the egg
- Becomes globally visible as "the Bearer"
- Cleared after 3 days offline (configurable)

### Dragon's Hunger (Ability)
- **Activation:** `/dl hunger on` (Bearer only)
- **Duration:** 5 minutes (configurable)
- **Cooldown:** 1 minute (configurable)
- **Grants:**
  - Strength II (+4 damage)
  - Speed II (2x speed)
  - +20 max health
  - +4 attack damage

### Passive Effects (While Holding Egg)
- Resistance I (50% damage reduction)
- Saturation I (constant food)
- +4 max health

### Egg Protections
✓ Void | ✓ Fire | ✓ Lava | ✓ Explosions | ✓ Cactus | ✓ Despawn | ✓ Hoppers | ✓ Portals

## Directory Structure Essentials

```
src/main/java/dev/dragonslegacy/
├── egg/                     # Core tracking & state
├── ability/                 # Dragon's Hunger system
├── config/                  # YAML configuration
├── command/                 # /dragonslegacy commands
├── announce/                # Message broadcasting
├── api/                     # Public API
├── mixin/                   # Deep Minecraft hooks
└── utils/                   # Utilities
```

## Configuration Files (7 total)

| File | Purpose | Key Settings |
|------|---------|--------------|
| `config.yaml` | Enable/disable | `enabled: true` |
| `egg.yaml` | Egg behavior | Protection flags, visibility modes, search radius |
| `ability.yaml` | Hunger ability | Duration, cooldown, effects, attributes |
| `passive.yaml` | Passive effects | Effects & attributes while holding |
| `messages.yaml` | Chat output | 19+ messages, channels (chat/actionbar/title), visibility |
| `commands.yaml` | Command names | Root command, aliases, subcommand names |
| `glow.yaml` | Egg glow system | Colors, crafting with materials |

## Commands

### Public
- `/dl help` - Show help
- `/dl bearer` - Who has the egg & location
- `/dl hunger on` - Activate ability (Bearer only)
- `/dl hunger off` - Deactivate ability

### Admin (OP required)
- `/dl reload` - Reload configs (no restart needed)
- `/dl info` - Full status
- `/dl test <key>` - Preview message
- `/dl placeholders` - Show placeholder values
- `/dl setbearer <player>` - Force bearer
- `/dl clearability` - Clear ability state
- `/dl resetcooldown` - Reset cooldown

## Placeholders (17 total)

| Placeholder | Output | Example |
|-------------|--------|---------|
| `%dragonslegacy:player%` | Your name | Steve |
| `%dragonslegacy:bearer%` | Egg holder | Alex |
| `%dragonslegacy:x%`, `y`, `z` | Coordinates | 120, 64, -340 |
| `%dragonslegacy:pos%` | Formatted position | ~120, ~64, ~-340 |
| `%dragonslegacy:global_prefix%` | Message prefix | [Dragon's Legacy] |
| `%dragonslegacy:egg_state%` | Egg state | inventory, item, placed, etc. |

## Visibility Modes

For egg location reporting:
- `EXACT` - Show precise coordinates
- `RANDOMIZED` - Show approximate (±2-3 blocks)
- `HIDDEN` - Don't show location

Can be configured per egg state (inventory, block, item, etc.)

## Message System

### Output Channels
- `chat` - Regular chat message
- `actionbar` - Above hotbar
- `title` - Large centered text
- `subtitle` - Below title
- `bossbar` - Color bar at top

### Visibility Options
- `everyone` - All players
- `bearer_only` - Just the bearer
- `everyone_except_bearer` - Everyone else
- `executor_only` - Command executor only

### Conditions
Messages can be conditional:
- `ability_active: true/false`
- `egg_held: true/false`
- `egg_placed: true/false`
- And more...

## Key Classes

| Class | Purpose |
|-------|---------|
| `DragonsLegacy.java` | Main coordinator singleton |
| `EggTracker.java` | Locates egg (searches all containers) |
| `EggIdentityManager.java` | Marks the canonical egg |
| `AbilityEngine.java` | Manages Dragon's Hunger |
| `PassiveEffectsEngine.java` | Applies passive bonuses |
| `ConfigManager.java` | Loads all YAML configs |
| `DragonsLegacyCommands.java` | Command registration |
| `MessageOutputSystem.java` | Sends messages to players |
| `EggPersistentState.java` | Saves state to disk |

## Architecture Patterns

### Subsystem Coordination
- `DragonsLegacy` singleton coordinates all subsystems
- Each subsystem is independent but wired together
- Event bus (DragonEggEventBus) publishes events

### Event-Driven
- Egg events: picked up, dropped, placed, bearer changed, teleported
- Ability events: activated, deactivated, expired, cooldown started/ended
- Other systems listen and react

### Configuration-First Design
- All behaviors configurable via YAML
- ConfigManager loads on startup and can reload
- Command names, messages, effects all customizable

### Recursive Egg Tracking
- EggTracker searches through:
  - Player inventories
  - Ender chests
  - All container blocks (chests, barrels, hoppers, etc.)
  - Shulker boxes, bundles
  - Animal containers (horses, llamas, camels)
  - Item entities on ground

## Integration Points

### Existing Integrations
- **BlueMap:** Customizable egg marker on web map
- **PlaceholderAPI:** All 17 placeholders available
- **LuckPerms:** Uses Fabric Permissions API
- **MiniMessage:** Rich text formatting (colors, hover, click)

### Public API
- `DragonEggAPI.java` - Query/update egg state
- `EventsApi.java` - Listen to events
- Other mods can integrate easily

## Persistence

Egg state is saved to:
- **Location:** Overworld's DimensionDataStorage
- **Survives:** Server restarts, dimension changes, backups
- **Includes:** Bearer UUID, coordinates, state, world ID, canonical egg ID

## Dependencies

```
Minecraft 1.21.11
├── Fabric Loader 0.18.4+
├── Fabric API 0.141.3+
├── Configurate 4.2.0 (YAML parsing)
├── Exp4j 0.4.8 (math expressions)
├── MiniMessage 4.17.0 (text formatting)
├── Fabric Permissions API 0.6.1
└── Placeholder API 2.8.1+
```

## Default Values Quick Reference

| Setting | Default | File |
|---------|---------|------|
| Ability Duration | 6000 ticks (5 min) | ability.yaml |
| Ability Cooldown | 1200 ticks (1 min) | ability.yaml |
| Offline Reset | 3 days | egg.yaml |
| Search Radius | 25 blocks | egg.yaml |
| Nearby Range | 64 blocks | egg.yaml |
| Block Ender Chest | true | egg.yaml |
| Block Elytra During | true | ability.yaml |
| Glow Enabled | true | glow.yaml |

## Performance Considerations

- **Mixins:** 16 strategically placed mixins for egg tracking
- **Recursive Search:** Optimized to cache results
- **Event-Driven:** Minimal per-tick overhead
- **YAML Reloading:** Can reload most configs without restart
- **Persistence:** Automatic saving, no manual commands needed

## File Statistics

- **Total Files:** 97 Java classes
- **Total Lines:** ~7,000 LOC
- **Config Files:** 7 YAML files
- **Documentation:** 14 wiki pages + README
- **Mixin Classes:** 16

## Wiki Pages (14 total)

1. Home.md - Overview & quick start
2. Installation.md - Setup instructions
3. Configuration.md - All config options detailed
4. Messages-and-Prefixes.md - Message system
5. Commands.md - All commands listed
6. Placeholders.md - All 17 placeholders
7. Ability-System.md - Dragon's Hunger deep-dive
8. Passive-Effects.md - Passive bonuses
9. Glow-System.md - Glow colors & crafting
10. Egg-Behavior-and-Protections.md - Protections & states
11. Persistence-and-States.md - Data saving
12. Migration-Guide.md - Config migration
13. Troubleshooting.md - Common issues
14. FAQ.md - Q&A

---

**Quick Fact:** Everything in this mod is configurable without code changes. Server owners can customize messages, commands, abilities, effects, and behavior entirely through YAML files.

