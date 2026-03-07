package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that resolves {@link AttributeEntry} objects from YAML config into
 * live Minecraft {@link Attribute} holders, {@link Identifier} modifier IDs, and
 * {@link AttributeModifier.Operation} values.
 *
 * <p>All methods log warnings for invalid/unknown entries and never throw.
 */
public final class ConfigAttributeParser {

    private ConfigAttributeParser() {}

    // -------------------------------------------------------------------------
    // Attribute lookup
    // -------------------------------------------------------------------------

    /**
     * Resolves the {@link Holder} for the attribute referenced by {@code entry.id}.
     *
     * @param entry the config entry to resolve
     * @return the attribute holder, or {@code null} if the entry is invalid or unknown
     */
    @Nullable
    public static Holder<Attribute> parseAttribute(AttributeEntry entry) {
        if (entry == null || entry.id == null || entry.id.isBlank()) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Attribute entry has null/empty id – skipping.");
            return null;
        }
        Identifier id;
        try {
            id = Identifier.parse(entry.id);
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Invalid attribute identifier '{}' – skipping: {}", entry.id, e.getMessage());
            return null;
        }
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, id))
            .map(h -> (Holder<Attribute>) h).orElseGet(() -> {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Unknown attribute '{}' – skipping.", entry.id);
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Modifier identifier
    // -------------------------------------------------------------------------

    /**
     * Builds a stable {@link Identifier} for the attribute modifier using the
     * {@code uuid} field of the entry.  The UUID string is used as the path
     * under the {@code dragonslegacy} namespace.
     *
     * @param entry the config entry whose {@code uuid} field is used
     * @return a valid {@link Identifier}, or {@code null} if the uuid is blank/invalid
     */
    @Nullable
    public static Identifier getModifierIdentifier(AttributeEntry entry) {
        if (entry == null || entry.uuid == null || entry.uuid.isBlank()) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Attribute entry has null/empty uuid – skipping.");
            return null;
        }
        try {
            // UUIDs contain hex digits and dashes – both valid in Identifier paths.
            String path = entry.uuid.toLowerCase();
            return Identifier.fromNamespaceAndPath("dragonslegacy", path);
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Invalid uuid '{}' for attribute '{}' – skipping: {}",
                entry.uuid, entry.id, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Operation parsing
    // -------------------------------------------------------------------------

    /**
     * Parses the {@code operation} string from a config entry into an
     * {@link AttributeModifier.Operation} enum value.
     *
     * <p>Accepted values (case-insensitive):
     * <ul>
     *   <li>{@code "add_value"} → {@link AttributeModifier.Operation#ADD_VALUE}</li>
     *   <li>{@code "multiply_base"} → {@link AttributeModifier.Operation#ADD_MULTIPLIED_BASE}</li>
     *   <li>{@code "multiply_total"} → {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}</li>
     * </ul>
     *
     * @param entry the config entry whose {@code operation} field is parsed
     * @return the resolved operation, or {@link AttributeModifier.Operation#ADD_VALUE} as fallback
     */
    public static AttributeModifier.Operation parseOperation(AttributeEntry entry) {
        if (entry == null || entry.operation == null) return AttributeModifier.Operation.ADD_VALUE;
        return switch (entry.operation.toLowerCase()) {
            case "multiply_base"  -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> {
                if (!"add_value".equalsIgnoreCase(entry.operation)) {
                    DragonsLegacyMod.LOGGER.warn(
                        "[Dragon's Legacy] Unknown operation '{}' for attribute '{}' – defaulting to add_value.",
                        entry.operation, entry.id);
                }
                yield AttributeModifier.Operation.ADD_VALUE;
            }
        };
    }
}
