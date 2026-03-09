# Installation

This page covers everything you need to get Dragon's Legacy running on your server or single-player world.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft (Java Edition) | **1.21.x** |
| Fabric Loader | **0.15.0** or newer |
| Fabric API | Latest for your MC version |

Dragon's Legacy has **no other hard dependencies**. Fabric API is the only required library.

> **Note:** The mod is server-side. Players connecting to a server do **not** need to install it in their own `mods/` folder. For single-player, install it normally.

---

## Downloading the Mod

1. Go to the mod's [GitHub Releases](https://github.com/iFamishedX/DragonEggGame/releases) page.
2. Make sure you select the file that matches your exact Minecraft version (e.g., `1.21.1`).
3. Download the `.jar` file whose name starts with `dragons-legacy-`.

---

## Installation Steps

### Server (Multiplayer)

1. **Stop your server** if it is currently running.
2. Place the `dragons-legacy-*.jar` file inside your server's `mods/` directory.
3. Confirm that `fabric-api-*.jar` is also present in `mods/`.
4. **Start the server**. Dragon's Legacy will generate its configuration directory at:
   ```
   config/dragonslegacy/
   ├── config.yaml
   ├── egg.yaml
   ├── ability.yaml
   ├── passive.yaml
   ├── glow.yaml
   ├── commands.yaml
   └── messages.yaml
   ```
5. The mod is now active. The first player to pick up the Dragon Egg will become the Bearer.

### Single-Player / LAN

1. Open the Minecraft Launcher and select your **Fabric** profile for the correct version.
2. Place the `dragons-legacy-*.jar` file inside your `.minecraft/mods/` directory (or the instance's `mods/` folder if you use a launcher like Prism or MultiMC).
3. Confirm `fabric-api-*.jar` is present in the same folder.
4. Launch the game. Config files are generated in `.minecraft/config/dragonslegacy/` on first run.

---

## Verifying the Installation

After starting the server or world, you should see a log line similar to:

```
[Dragon's Legacy] Loaded config: config.yaml (version 1)
[Dragon's Legacy] Loaded config: egg.yaml (version 1)
...
[Dragon's Legacy] Dragon's Legacy enabled.
```

In-game, run:

```
/dl help
```

If you see the help menu, the mod is working correctly.

---

## Installing Fabric Loader (if needed)

1. Go to [https://fabricmc.net/use/](https://fabricmc.net/use/).
2. Select **Minecraft Version** `1.21.x` and download the installer.
3. Run the installer and select **Install for server** or **Install for client** as appropriate.
4. Follow the on-screen steps.

Fabric API is downloaded separately from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).

---

## Updating the Mod

1. **Stop the server** (or close the game for single-player).
2. Delete the old `dragons-legacy-*.jar` from `mods/`.
3. Place the new `dragons-legacy-*.jar` into `mods/`.
4. Check the [Migration Guide](Migration-Guide.md) for any config changes introduced in the new version.
5. Start the server. If a config file has a new `config_version`, the mod will log a warning and use defaults for any new/missing keys — your old values are preserved.
6. Review and update your config files as needed, then run `/dl reload`.

> **Tip:** Always back up your `config/dragonslegacy/` folder and your world data before updating.

---

## Uninstalling

1. Stop the server.
2. Remove `dragons-legacy-*.jar` from `mods/`.
3. Optionally delete the `config/dragonslegacy/` directory if you no longer need your settings.

The mod stores no data outside of the config directory and the world's `data/` folder. No database or external service is used.

---

## Troubleshooting Installation

| Symptom | Likely Cause | Fix |
|---|---|---|
| Server crashes on startup | Mismatched Minecraft / Fabric version | Re-download the correct mod jar |
| `/dl` command not found | Mod not loaded | Check `mods/` directory, verify Fabric API is present |
| Config files not generated | Mod failed to initialize | Check server logs for errors |
| `ClassNotFoundException` in logs | Missing Fabric API | Add `fabric-api-*.jar` to `mods/` |

See [Troubleshooting](Troubleshooting.md) for more detailed help.
