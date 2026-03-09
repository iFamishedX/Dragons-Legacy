# Infusion System

The Infusion System lets the Dragon Egg emit a colored glow. The egg glows white by default, and the holder can permanently change the glow color by combining the egg with a specific material in an **anvil**.

---

## Configuration File

```
config/dragonslegacy/infusion.yaml
```

Default configuration:

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

    iron_ingot:
      color: "#D8D8D8"
      tooltip: "Infused with Iron"

    netherite_ingot:
      color: "#3C2A23"
      tooltip: "Infused with Netherite"

    quartz:
      color: "#FFFFFF"
      tooltip: "Infused with Quartz"

    redstone:
      color: "#FF0000"
      tooltip: "Infused with Redstone"

    emerald:
      color: "#00FF55"
      tooltip: "Infused with Emerald"

    diamond:
      color: "#00FFFF"
      tooltip: "Infused with Diamond"
```

---

## Enabling / Disabling

| Key | Effect |
|---|---|
| `infusion.enabled: true` | The egg glows with its stored color |
| `infusion.enabled: false` | Glow is completely disabled; no color is applied |

---

## Default Glow Color

```yaml
infusion:
  default_color: "#FFFFFF"
```

This is the color the egg starts with before any infusion. Set it to any hex color to give your server a unique default:

```yaml
infusion:
  default_color: "#8800FF"   # Deep purple default
```

---

## Anvil Infusion

The Dragon Egg is always the base item (hardcoded). To infuse a color:

1. Open an **anvil**.
2. Place the **Dragon Egg** in the left slot.
3. Place the desired **material** in the right slot.
4. Take the result from the output slot.

The egg's glow color is updated immediately. The material is consumed.

> The anvil shows an experience cost as normal.

---

## Default Material Reference

| Material | Item ID | Color | Tooltip |
|---|---|---|---|
| Amethyst Shard | `amethyst_shard` | `#AA00FF` (Purple) | Infused with Amethyst |
| Copper Ingot | `copper_ingot` | `#B87333` (Copper) | Infused with Copper |
| Gold Ingot | `gold_ingot` | `#FFD700` (Gold) | Infused with Gold |
| Iron Ingot | `iron_ingot` | `#D8D8D8` (Silver) | Infused with Iron |
| Netherite Ingot | `netherite_ingot` | `#3C2A23` (Dark Brown) | Infused with Netherite |
| Quartz | `quartz` | `#FFFFFF` (White) | Infused with Quartz |
| Redstone | `redstone` | `#FF0000` (Red) | Infused with Redstone |
| Emerald | `emerald` | `#00FF55` (Green) | Infused with Emerald |
| Diamond | `diamond` | `#00FFFF` (Cyan) | Infused with Diamond |

---

## Adding Custom Materials

Add any item as a crafting material. Item namespace is auto-completed (`minecraft:` is added if missing):

```yaml
materials:
  lapis_lazuli:
    color: "#1F51FF"
    tooltip: "Infused with Lapis"

  blaze_powder:
    color: "#FF6600"
    tooltip: "Infused with Blaze"
```

The `tooltip` text is rendered in the infusion color.

---

## Hex Color Format

All colors are expressed as HTML hex color strings: `"#RRGGBB"`.

**Examples:**

| Color | Hex |
|---|---|
| White | `#FFFFFF` |
| Hot Pink | `#FF69B4` |
| Lava Orange | `#FF4500` |
| Void Purple | `#6A0DAD` |

---

## Notes

- The infusion color is stored **on the egg item itself**, so it persists when the egg is dropped, placed, or picked up.
- The tooltip is rendered in the infusion color in the item's lore.
- Each anvil operation overwrites the previous color.
- The color persists across server restarts.
