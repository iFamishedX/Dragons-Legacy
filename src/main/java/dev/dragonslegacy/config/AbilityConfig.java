package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class AbilityConfig {

    @Comment("Duration of the Dragon's Hunger ability in ticks (20 ticks = 1 second). Default: 6000 (5 min).")
    @Setting("duration_ticks")
    public int durationTicks = 6000;

    @Comment("Cooldown after Dragon's Hunger expires, in ticks. Default: 1200 (1 min).")
    @Setting("cooldown_ticks")
    public int cooldownTicks = 1200;

    @Comment("Status effects applied while Dragon's Hunger is active.")
    public List<EffectEntry> effects = createDefaultEffects();

    @Comment("Attribute modifiers applied while Dragon's Hunger is active.")
    public List<AttributeEntry> attributes = createDefaultAttributes();

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    private static List<EffectEntry> createDefaultEffects() {
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

        EffectEntry hunger = new EffectEntry();
        hunger.id = "minecraft:hunger";
        hunger.amplifier = 1;
        hunger.showParticles = false;
        hunger.showIcon = false;
        list.add(hunger);

        return list;
    }

    private static List<AttributeEntry> createDefaultAttributes() {
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
