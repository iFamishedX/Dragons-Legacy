package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that resolves {@link EffectEntry} objects from YAML config into
 * live Minecraft {@link MobEffect} holders and {@link MobEffectInstance} objects.
 *
 * <p>All methods log warnings for invalid/unknown entries and never throw.
 */
public final class ConfigEffectParser {

    /**
     * Duration used for passive effects (effectively infinite; avoids ever expiring
     * during normal play while still allowing clean removal via {@link net.minecraft.world.entity.LivingEntity#removeEffect}).
     */
    public static final int PASSIVE_DURATION = Integer.MAX_VALUE / 2;

    private ConfigEffectParser() {}

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    /**
     * Resolves the {@link Holder} for the effect referenced by {@code entry.id}.
     *
     * @param entry the config entry to resolve
     * @return the effect holder, or {@code null} if the entry is invalid or unknown
     */
    @Nullable
    public static Holder<MobEffect> parseEffect(EffectEntry entry) {
        if (entry == null || entry.id == null || entry.id.isBlank()) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Effect entry has null/empty id – skipping.");
            return null;
        }
        Identifier id;
        try {
            id = Identifier.parse(entry.id);
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Invalid effect identifier '{}' – skipping: {}", entry.id, e.getMessage());
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, id))
            .map(h -> (Holder<MobEffect>) h).orElseGet(() -> {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Unknown effect '{}' – skipping.", entry.id);
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Instance creation
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link MobEffectInstance} from a resolved holder and entry, using
     * the supplied {@code duration} (in ticks).
     *
     * @param effect   the resolved effect holder (must not be null)
     * @param entry    the config entry that controls amplifier/visibility
     * @param duration duration in ticks
     * @return a new {@link MobEffectInstance}
     */
    public static MobEffectInstance createInstance(Holder<MobEffect> effect, EffectEntry entry, int duration) {
        return new MobEffectInstance(effect, duration, entry.amplifier, false, entry.showParticles, entry.showIcon);
    }
}
