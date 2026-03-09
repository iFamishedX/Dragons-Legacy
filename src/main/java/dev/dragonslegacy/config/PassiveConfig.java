package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Passive effects configuration loaded from {@code config/dragonslegacy/passive.yaml}.
 * Contains effects and attribute modifiers always applied while the bearer holds the Dragon Egg.
 */
@ConfigSerializable
public class PassiveConfig {

    @Setting("config_version")
    public int configVersion = 1;

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
