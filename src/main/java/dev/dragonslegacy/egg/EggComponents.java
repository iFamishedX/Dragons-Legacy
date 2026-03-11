package dev.dragonslegacy.egg;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import static dev.dragonslegacy.DragonsLegacyMod.MOD_ID;

/**
 * Holds the {@code dragonslegacy:egg} data-component type used to identify the
 * canonical Dragon Egg item stack.
 *
 * <p>A Dragon Egg ItemStack is the authoritative egg if and only if this
 * component is present and set to {@code true}.  All detection code must go
 * through {@link EggCore#isDragonEgg(net.minecraft.world.item.ItemStack)}.
 */
public final class EggComponents {

    /**
     * Boolean data component that marks a Dragon Egg {@link net.minecraft.world.item.ItemStack}
     * as the canonical egg for Dragon's Legacy.
     *
     * <ul>
     *   <li>Namespace: {@code dragonslegacy}</li>
     *   <li>Key: {@code egg}</li>
     *   <li>Type: boolean</li>
     *   <li>Default: {@code false} (absent)</li>
     * </ul>
     */
    public static final DataComponentType<Boolean> EGG = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "egg"),
        DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .build()
    );

    /**
     * Call once during mod initialisation to ensure the component type is
     * registered (triggers static initialisation of this class).
     */
    public static void register() {
        // static initialiser above performs the registration
    }

    private EggComponents() {}
}
