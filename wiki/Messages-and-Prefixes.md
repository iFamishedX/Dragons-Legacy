# Messages and Prefixes

Dragon's Legacy delivers all player-facing text through `messages.yaml`. Every message is fully customizable: you can change the text, the delivery channel (chat, action bar, title, etc.), who sees it, and when it fires.

All text is rendered using **MiniMessage**, which means you can use color tags, gradients, hover events, click events, and more anywhere in your messages.

---

## File Location

```
config/dragonslegacy/messages.yaml
```

---

## Global Prefix

The `prefix` key at the top of the file defines a string that is automatically available as the `%dragonslegacy:global_prefix%` placeholder inside message texts.

```yaml
prefix: "<dark_gray>[<gradient:#AA00FF:#00FFFF>Dragon's Legacy</gradient>]</dark_gray> "
```

Using this placeholder in your messages keeps the prefix consistent and easy to update:

```yaml
text: "%dragonslegacy:global_prefix%<white>The bearer is <yellow>%dragonslegacy:bearer%</yellow>."
```

---

## Message Entry Structure

Each named entry under `messages:` follows this structure:

```yaml
messages:
  <message_key>:
    disabled: false
    order: <integer>
    cooldown_ticks: <integer>
    global_cooldown_ticks: <integer>
    conditions: <map>
    channels:
      - mode: <string>
        visibility: <string>
        text: <MiniMessage string>
```

### Top-Level Fields

| Field | Type | Description |
|---|---|---|
| `disabled` | boolean | When `true`, this message is silenced entirely without removing it from the file |
| `order` | integer | Sort order when multiple messages are displayed in sequence (lower = first) |
| `cooldown_ticks` | integer | Per-player cooldown in ticks before this message can fire again for the same player (0 = no cooldown) |
| `global_cooldown_ticks` | integer | Server-wide cooldown in ticks before this message can fire for any player (0 = no cooldown) |
| `conditions` | map | State-based conditions that must be satisfied before the message fires |
| `channels` | list | One or more delivery channels for the message |

---

## Message Keys

The following message keys are built-in:

| Key | When It Fires |
|---|---|
| `egg_picked_up` | A player picks up the Dragon Egg |
| `egg_dropped` | The Dragon Egg is dropped |
| `egg_placed` | The Dragon Egg is placed as a block |
| `egg_teleported` | The Dragon Egg is returned to spawn |
| `bearer_changed` | A new bearer picks up the egg |
| `bearer_cleared` | The bearer is cleared (no current bearer) |
| `ability_activated` | Dragon's Hunger is activated |
| `ability_deactivated` | Dragon's Hunger is manually deactivated |
| `ability_expired` | Dragon's Hunger expires naturally |
| `ability_cooldown_started` | The ability cooldown begins |
| `ability_cooldown_ended` | The ability cooldown ends |
| `not_bearer` | A non-bearer tries to use a bearer-only command |
| `elytra_blocked` | The bearer tries to use an Elytra while the ability is active |
| `help` | The `/dl help` command is run |
| `bearer_info` | The `/dl bearer` command with a known bearer |
| `bearer_none` | The `/dl bearer` command with no current bearer |

---

## Channels

Each entry in the `channels` list describes one way the message is sent.

### Channel Fields

| Field | Required | Description |
|---|---|---|
| `mode` | Yes | How the message is delivered (see Modes below) |
| `visibility` | Yes | Who receives the message (see Visibility below) |
| `text` | Yes | The message text in MiniMessage format |

A single message can have **multiple channels**:

```yaml
ability_activated:
  disabled: false
  channels:
    - mode: title
      visibility: bearer_only
      text: "<#FF4500><bold>Dragon's Hunger!</bold>"
    - mode: chat
      visibility: everyone
      text: "%dragonslegacy:global_prefix%%dragonslegacy:player% activated Dragon's Hunger."
```

---

## Delivery Modes

| Mode | Description |
|---|---|
| `chat` | Standard chat message |
| `actionbar` | Text above the hotbar |
| `title` | Large title text in the center of the screen |
| `subtitle` | Smaller text below the title |
| `bossbar` | Boss health bar at the top of the screen |

---

## Visibility

| Value | Who Receives the Message |
|---|---|
| `everyone` | All online players |
| `bearer_only` | Only the current bearer |
| `everyone_except_bearer` | All players except the bearer |
| `executor_only` | Only the player who triggered the command/event |
| `everyone_except_executor` | All players except the triggering player |

---

## Disabling a Message

Set `disabled: true` on any message to silence it without removing it from the config:

```yaml
ability_cooldown_started:
  disabled: true
  channels:
    - mode: chat
      visibility: everyone
      text: "..."
```

---

## Conditions

The `conditions` map lets you gate a message behind state checks:

```yaml
conditions:
  ability_active: true
  egg_held: false
```

Multiple conditions are combined with logical AND — all must pass.

---

## MiniMessage Formatting

All text fields support the full [MiniMessage](https://docs.advntr.dev/minimessage/) specification. MiniMessage is **always enabled**.

### Common Tags

| Tag | Result |
|---|---|
| `<red>text</red>` | Red colored text |
| `<bold>text</bold>` | Bold text |
| `<gradient:#FF0000:#0000FF>text</gradient>` | Color gradient |
| `<hover:show_text:'tooltip'>text</hover>` | Hover tooltip |
| `<click:run_command:'/dl bearer'>text</click>` | Clickable text |
| `<newline>` | Line break |
| `<reset>` | Clear all formatting |

### Example: Bearer Announcement

```yaml
bearer_changed:
  disabled: false
  order: 0
  cooldown_ticks: 0
  global_cooldown_ticks: 200
  conditions: {}
  channels:
    - mode: chat
      visibility: everyone
      text: "%dragonslegacy:global_prefix%<yellow><bold>%dragonslegacy:bearer%</bold></yellow> <white>has claimed the Dragon Egg!</white>"
    - mode: title
      visibility: bearer_only
      text: "<gold><bold>You are the Bearer!</bold></gold>"
    - mode: subtitle
      visibility: bearer_only
      text: "<gray>Hold the egg to receive its power.</gray>"
```

---

## Using Placeholders in Messages

Any placeholder from the [Placeholders](Placeholders.md) page can be used in any `text` value.

```yaml
text: "Bearer: %dragonslegacy:bearer% | Location: %dragonslegacy:egg_location%"
```

See [Placeholders](Placeholders.md) for the full list.

---

## Cooldown Tips

- Set `cooldown_ticks: 0` and `global_cooldown_ticks: 0` for instantaneous triggers like command responses.
- Use `global_cooldown_ticks` for server-wide announcements to avoid spam.
- `cooldown_ticks` is useful for per-player warnings so the same player is not spammed.
