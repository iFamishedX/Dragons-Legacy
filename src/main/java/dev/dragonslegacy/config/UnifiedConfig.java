package dev.dragonslegacy.config;

import dev.dragonslegacy.api.DragonEggAPI.PositionType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified configuration for Dragon's Legacy, loaded from {@code config/dragonslegacy/config.yaml}.
 * Contains all settings previously spread across main.yaml, ability.yaml, passive_effects.yaml,
 * commands.yaml, and spawn.yaml.
 */
@ConfigSerializable
public class UnifiedConfig {

    public EggSection egg = new EggSection();
    public AbilitySection ability = new AbilitySection();
    public PassiveSection passive = new PassiveSection();
    public CommandsSection commands = new CommandsSection();

    // -------------------------------------------------------------------------
    // Egg section
    // -------------------------------------------------------------------------

    /** Settings controlling egg behavior, protection, visibility, and offline reset. */
    @ConfigSerializable
    public static class EggSection {

        @Setting("search_radius")
        public float searchRadius = 25f;

        @Setting("block_ender_chest")
        public boolean blockEnderChest = true;

        @Setting("block_container_items")
        public boolean blockContainerItems = false;

        @Setting("offline_reset_days")
        public double offlineResetDays = 3.0;

        @Setting("nearby_range")
        public int nearbyRange = 64;

        public Map<PositionType, VisibilityType> visibility = buildDefaultVisibility();

        private static Map<PositionType, VisibilityType> buildDefaultVisibility() {
            Map<PositionType, VisibilityType> map = new LinkedHashMap<>();
            map.put(PositionType.INVENTORY,     VisibilityType.EXACT);
            map.put(PositionType.ITEM,          VisibilityType.EXACT);
            map.put(PositionType.PLAYER,        VisibilityType.HIDDEN);
            map.put(PositionType.BLOCK,         VisibilityType.RANDOMIZED);
            map.put(PositionType.FALLING_BLOCK, VisibilityType.EXACT);
            map.put(PositionType.ENTITY,        VisibilityType.EXACT);
            return map;
        }
    }

    // -------------------------------------------------------------------------
    // Ability section
    // -------------------------------------------------------------------------

    /** Settings for Dragon's Hunger: duration, cooldown, elytra blocking, active effects and attributes. */
    @ConfigSerializable
    public static class AbilitySection {

        @Setting("duration_ticks")
        public int durationTicks = 6000;

        @Setting("cooldown_ticks")
        public int cooldownTicks = 1200;

        @Setting("block_elytra")
        public boolean blockElytra = true;

        public List<EffectEntry> effects = buildDefaultEffects();
        public List<AttributeEntry> attributes = buildDefaultAttributes();

        private static List<EffectEntry> buildDefaultEffects() {
            List<EffectEntry> list = new ArrayList<>();
            EffectEntry strength = new EffectEntry();
            strength.id = "minecraft:strength";
            strength.amplifier = 1;
            strength.showParticles = true;
            strength.showIcon = true;
            list.add(strength);
            EffectEntry speed = new EffectEntry();
            speed.id = "minecraft:speed";
            speed.amplifier = 1;
            speed.showParticles = true;
            speed.showIcon = true;
            list.add(speed);
            return list;
        }

        private static List<AttributeEntry> buildDefaultAttributes() {
            List<AttributeEntry> list = new ArrayList<>();
            AttributeEntry health = new AttributeEntry();
            health.id = "minecraft:max_health";
            health.amount = 20.0;
            health.operation = "add_value";
            list.add(health);
            AttributeEntry damage = new AttributeEntry();
            damage.id = "minecraft:attack_damage";
            damage.amount = 4.0;
            damage.operation = "add_value";
            list.add(damage);
            return list;
        }
    }

    // -------------------------------------------------------------------------
    // Passive section
    // -------------------------------------------------------------------------

    /** Passive effects and attribute modifiers always applied while the bearer holds the egg. */
    @ConfigSerializable
    public static class PassiveSection {

        public List<EffectEntry> effects = buildDefaultEffects();
        public List<AttributeEntry> attributes = buildDefaultAttributes();

        private static List<EffectEntry> buildDefaultEffects() {
            List<EffectEntry> list = new ArrayList<>();
            EffectEntry resistance = new EffectEntry();
            resistance.id = "minecraft:resistance";
            resistance.amplifier = 0;
            resistance.showParticles = false;
            resistance.showIcon = false;
            list.add(resistance);
            EffectEntry saturation = new EffectEntry();
            saturation.id = "minecraft:saturation";
            saturation.amplifier = 0;
            saturation.showParticles = false;
            saturation.showIcon = false;
            list.add(saturation);
            return list;
        }

        private static List<AttributeEntry> buildDefaultAttributes() {
            List<AttributeEntry> list = new ArrayList<>();
            AttributeEntry health = new AttributeEntry();
            health.id = "minecraft:max_health";
            health.amount = 4.0;
            health.operation = "add_value";
            list.add(health);
            return list;
        }
    }

    // -------------------------------------------------------------------------
    // Commands section
    // -------------------------------------------------------------------------

    /** Root command name, aliases, and configurable subcommand names. */
    @ConfigSerializable
    public static class CommandsSection {

        public String root = "dragonslegacy";
        public List<String> aliases = new ArrayList<>(List.of("dl"));
        public SubcommandNames subcommands = new SubcommandNames();

        @ConfigSerializable
        public static class SubcommandNames {
            public String help = "help";
            public String bearer = "bearer";
            public String hunger = "hunger";

            @Setting("hunger_on")
            public String hungerOn = "on";

            @Setting("hunger_off")
            public String hungerOff = "off";
        }
    }
}
