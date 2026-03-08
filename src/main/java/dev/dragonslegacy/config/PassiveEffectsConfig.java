package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for passive effects applied to the Dragon Egg bearer
 * (without needing Dragon's Hunger active).
 *
 * <p>Loaded from {@code config/dragonslegacy/passive_effects.yaml}.
 */
@ConfigSerializable
public class PassiveEffectsConfig {

    @Comment("Status effects applied while holding the Dragon Egg (no ability needed).")
    public List<EffectEntry> effects = createDefaultEffects();

    @Comment("Attribute modifiers applied while holding the Dragon Egg (no ability needed).")
    public List<AttributeEntry> attributes = createDefaultAttributes();

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    private static List<EffectEntry> createDefaultEffects() {
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

        EffectEntry glowing = new EffectEntry();
        glowing.id = "minecraft:glowing";
        glowing.amplifier = 0;
        glowing.showParticles = false;
        glowing.showIcon = false;
        list.add(glowing);

        return list;
    }

    private static List<AttributeEntry> createDefaultAttributes() {
        List<AttributeEntry> list = new ArrayList<>();

        AttributeEntry health = new AttributeEntry();
        health.id = "minecraft:max_health";
        health.amount = 4.0;
        health.operation = "add_value";
        list.add(health);

        AttributeEntry speed = new AttributeEntry();
        speed.id = "minecraft:movement_speed";
        speed.amount = 0.05;
        speed.operation = "multiply_total";
        list.add(speed);

        return list;
    }
}
