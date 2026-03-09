# Dragon's Legacy — Wiki Home

Welcome to the **Dragon's Legacy** wiki! Dragon's Legacy is a Fabric mod for Minecraft 1.21.x that transforms the Dragon Egg into a living, persistent, server-wide relic. One player at a time can carry the egg, gaining powerful bonuses and becoming the **Bearer** — a title that every player on the server can see, track, and challenge.

---

## What Does This Mod Do?

When someone picks up the Dragon Egg for the first time, the mod marks them as the Bearer. While holding the egg they gain passive bonuses, and they can activate **Dragon's Hunger** — a powerful 5-minute ability that dramatically boosts their combat stats. The egg itself is carefully protected against every form of destruction the game can throw at it, and its location is tracked persistently even across server restarts and dimension changes.

Everything about the mod — from effect strengths, to command names, to every line of chat text the server sends — is configurable through a set of well-organized YAML files.

---

## Feature Overview

| Feature | Summary |
|---|---|
| **Bearer System** | Tracks which player currently holds the Dragon Egg across sessions |
| **Dragon's Hunger** | Activatable ability: Strength II, Speed II, +20 max health, +4 attack damage for 5 minutes |
| **Passive Effects** | Resistance I, Saturation I, +4 max health while holding the egg |
| **Infusion System** | Combine the egg with materials in an anvil to change its glow color and tooltip |
| **Egg Protections** | Immune to void, fire, lava, explosions, cactus, hopper pickup, portal teleportation, and despawn |
| **Visibility Modes** | Control how precisely the egg's location is revealed (EXACT, RANDOMIZED, HIDDEN) |
| **Egg States** | HELD, DROPPED, PLACED, ENTITY, UNKNOWN — all persisted to disk |
| **7 Config Files** | Granular control over every behavior, broken into focused files |
| **Permission API** | Use LuckPerms permission nodes or vanilla op levels (configured per-command in global.yaml) |
| **Full Command Suite** | `/dragonslegacy` (alias `/dl`) with subcommands for all management tasks |
| **17 Placeholders** | Rich placeholder support for messages and integrations |
| **MiniMessage Formatting** | All text uses MiniMessage — gradients, hover events, click events, and more |

---

## Wiki Pages

| Page | Description |
|---|---|
| **[Installation](Installation.md)** | Requirements, dependencies, and step-by-step setup |
| **[Configuration](Configuration.md)** | All 7 config files explained with annotated examples |
| **[Messages and Prefixes](Messages-and-Prefixes.md)** | How messages.yaml works: channels, visibility, conditions, and formatting |
| **[Commands](Commands.md)** | Every command, permission node, and usage example |
| **[Placeholders](Placeholders.md)** | All `%dragonslegacy:*%` placeholders with descriptions and example output |
| **[Ability System](Ability-System.md)** | Dragon's Hunger deep-dive: effects, attributes, and elytra blocking |
| **[Passive Effects](Passive-Effects.md)** | What the bearer receives just from holding the egg |
| **[Infusion System](Glow-System.md)** | Anvil infusion recipes, supported materials, and custom colors |
| **[Egg Behavior and Protections](Egg-Behavior-and-Protections.md)** | Every protection flag, visibility mode, and egg-state behavior |
| **[Persistence and States](Persistence-and-States.md)** | What is saved to disk, when, and how it survives restarts |
| **[Migration Guide](Migration-Guide.md)** | Upgrading from a previous config layout to the current one |
| **[Troubleshooting](Troubleshooting.md)** | Common issues, validation warnings, and diagnostic commands |
| **[FAQ](FAQ.md)** | Frequently asked questions answered concisely |

---

## Quick Start

1. Drop `dragons-legacy-*.jar` into your `mods/` folder.
2. Start the server once to generate all config files under `config/dragonslegacy/`.
3. Edit the files to your liking (see [Configuration](Configuration.md)).
4. Run `/dl reload` to apply changes without restarting.
5. The first player to pick up the Dragon Egg becomes the Bearer.

For full installation instructions see **[Installation](Installation.md)**.

---

## Requirements at a Glance

- Minecraft **1.21.x**
- **Fabric Loader** 0.15+
- **Fabric API**

---

## Links

- [Source code on GitHub](https://github.com/iFamishedX/Dragons-Legacy)
- [Issue tracker](https://github.com/iFamishedX/Dragons-Legacy/issues)
- [Releases](https://github.com/iFamishedX/Dragons-Legacy/releases)
