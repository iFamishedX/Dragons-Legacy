package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragon's Hunger ability configuration loaded from {@code config/dragonslegacy/ability.yaml}.
 */
@ConfigSerializable
public class AbilityFileConfig {

    @Setting("config_version")
    public int configVersion = 1;

    @Setting("duration_ticks")
    public int durationTicks = 6000;

    @Setting("cooldown_ticks")
    public int cooldownTicks = 1200;

    @Setting("block_elytra")
    public boolean blockElytra = true;

    public ScalingSection scaling = new ScalingSection();

    public List<EffectEntry> effects = buildDefaultEffects();

    public List<AttributeEntry> attributes = buildDefaultAttributes();

    /** Scaling multipliers based on server population or other metrics. */
    @ConfigSerializable
    public static class ScalingSection {
        public boolean enabled = false;

        @Setting("health_multiplier")
        public double healthMultiplier = 0.0;

        @Setting("damage_multiplier")
        public double damageMultiplier = 0.0;

        @Setting("speed_multiplier")
        public double speedMultiplier = 0.0;
    }

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
