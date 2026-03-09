# FAQ

Frequently asked questions about Dragon's Legacy, organized by topic.

---

## General

### What is Dragon's Legacy?

Dragon's Legacy is a Fabric mod for Minecraft 1.21.x that transforms the Dragon Egg into a persistent, server-wide relic. One player at a time is designated the **Bearer** — they receive passive bonuses and can activate a powerful active ability called **Dragon's Hunger**. The egg's location is tracked, protected from destruction, and persisted across server restarts.

---

### Is this a client-side or server-side mod?

Dragon's Legacy is a **server-side mod**. Players do not need to install it themselves.

---

### Does Dragon's Legacy work with other mods?

Generally yes. The mod hooks into vanilla Minecraft mechanics (item entities, blocks, status effects, attributes). Mods that heavily modify inventory behavior or item entities may interfere with protections. Report compatibility issues on the issue tracker.

---

## Bearer System

### How does a player become the Bearer?

The first player to pick up the Dragon Egg after the mod initializes becomes the Bearer. The next player to pick it up after the egg is dropped becomes the new Bearer.

---

### Can there be multiple Bearers?

No. There is exactly one Bearer at any time.

---

### What happens if the Bearer logs off?

The Bearer's identity is saved to disk and persists across restarts.

---

## Dragon's Hunger

### Can anyone use Dragon's Hunger?

Only the current Bearer can activate the ability with `/dl hunger on`.

---

### Does the ability stack with passive effects?

Yes. Both passive bonuses (while holding the egg) and the active ability bonuses apply simultaneously. With defaults: +4 HP (passive) + +20 HP (ability) = +24 HP (12 extra hearts).

---

## Egg Protections

### My egg was destroyed. Why?

Check the `protection` flags in `egg.yaml`. Each flag must be `true` to protect against that hazard. Run `/dl reload` after enabling any flag.

---

### Can players use hoppers to steal the egg?

Not when `protection.hopper: true` (the default).

---

## Infusion System

### How do I change the egg's glow color?

Place the Dragon Egg in the left slot of an anvil and the desired material in the right slot. The output will have the new glow color and tooltip. See [Infusion System](Glow-System.md) for the full material list.

---

### Can I add my own infusion materials?

Yes. Add entries to the `materials` map in `infusion.yaml` with a `color` (hex) and `tooltip` (string). Any vanilla item can be used; item namespace is auto-completed.

---

### Can I disable the glow entirely?

Yes. Set `infusion.enabled: false` in `infusion.yaml` and run `/dl reload`.

---

## Configuration

### Do I need to restart the server after editing config files?

For most files (`messages.yaml`, `egg.yaml`, `ability.yaml`, `passive.yaml`, `infusion.yaml`, `logging.yaml`): **No.** Run `/dl reload`.

For `global.yaml` command names and aliases: **Yes.** A full server restart is required because Fabric registers commands at startup.

---

### What format do I use for text in messages.yaml?

All text uses **MiniMessage** format. Legacy `&` color codes are not supported. See [Messages and Prefixes](Messages-and-Prefixes.md) for full details.

---

### How do I silence a specific message?

Set `disabled: true` on the message entry in `messages.yaml`:

```yaml
ability_cooldown_started:
  disabled: true
  channels: ...
```

---

### How do I control who can run commands?

Configure `permissions_api` and per-command settings in `global.yaml`:

```yaml
permissions_api: true  # uses LuckPerms nodes

commands:
  reload:
    permission_node: "dragonslegacy.command.reload"
    op_level: 3  # used when permissions_api: false
```

---

## Placeholders

### Where can I use placeholders?

In any `text` field in `messages.yaml`. Also displayed by `/dl placeholders` for debugging.

---

### A placeholder is showing as empty. Why?

- Location placeholders return empty when visibility is `HIDDEN`.
- `%dragonslegacy:bearer%` returns `none` when there is no bearer.
- `%dragonslegacy:ability_duration%` returns `0` when the ability is not active.

Run `/dl placeholders` to see all current values.
