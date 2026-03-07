package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Represents a single attribute-modifier entry in a Dragon's Legacy YAML config.
 *
 * <p>Used by both {@link PassiveEffectsConfig} (passive bearer attributes) and
 * {@link AbilityConfig} (Dragon's Hunger ability attributes).
 *
 * <p>The modifier identifier is derived automatically from {@code id} and {@code operation}
 * via {@link dev.dragonslegacy.config.ConfigAttributeParser#getModifierIdentifier}; no UUID
 * field is needed in the YAML.
 */
@ConfigSerializable
public class AttributeEntry {

    /**
     * Namespaced identifier of the attribute, e.g. {@code "minecraft:max_health"}.
     * A bare name without a namespace (e.g. {@code "max_health"}) is also accepted;
     * {@code "minecraft:"} will be prepended automatically.
     */
    public String id = "minecraft:max_health";

    /** Amount to add/multiply. */
    public double amount = 0.0;

    /**
     * Operation applied to the attribute.
     * Accepted values: {@code "add_value"}, {@code "multiply_base"}, {@code "multiply_total"}.
     */
    public String operation = "add_value";
}
