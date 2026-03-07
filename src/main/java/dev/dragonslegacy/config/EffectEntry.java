package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Represents a single status-effect entry in a Dragon's Legacy YAML config.
 *
 * <p>Used by both {@link PassiveEffectsConfig} (passive bearer effects) and
 * {@link AbilityConfig} (Dragon's Hunger ability effects).
 */
@ConfigSerializable
public class EffectEntry {

    /**
     * Namespaced identifier of the effect, e.g. {@code "minecraft:speed"}.
     * A bare name without a namespace (e.g. {@code "speed"}) is also accepted;
     * {@code "minecraft:"} will be prepended automatically.
     */
    public String id = "minecraft:speed";

    /** Effect amplifier (0 = level I, 1 = level II, …). */
    public int amplifier = 0;

    /** Whether to show particle effects on the player. */
    @Setting("show_particles")
    public boolean showParticles = true;

    /** Whether to show the effect icon in the HUD. */
    @Setting("show_icon")
    public boolean showIcon = true;
}
