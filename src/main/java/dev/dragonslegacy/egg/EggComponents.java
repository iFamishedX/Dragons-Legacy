package dev.dragonslegacy.egg;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.UUID;

import static dev.dragonslegacy.DragonsLegacyMod.MOD_ID;

/**
 * Holds the data-component types used to identify the canonical Dragon Egg item stack.
 *
 * <ul>
 *   <li>{@link #EGG} – boolean flag marking participation in the Dragon's Legacy egg system</li>
 *   <li>{@link #EGG_ID} – hidden UUID that matches the world-persistent canonical ID;
 *       used by the scrub pass to detect counterfeits and is scrambled periodically</li>
 * </ul>
 *
 * <p>The authoritative identity check is {@link EggCore#isDragonEgg(net.minecraft.world.item.ItemStack)};
 * full canonical verification (including UUID check) goes through
 * {@link EggCore#isCanonicalEgg(net.minecraft.world.item.ItemStack, UUID)}.
 */
public final class EggComponents {

    /**
     * Boolean data component that marks a Dragon Egg {@link net.minecraft.world.item.ItemStack}
     * as participating in the Dragon's Legacy egg system.
     *
     * <ul>
     *   <li>Namespace: {@code dragonslegacy}</li>
     *   <li>Key: {@code egg}</li>
     *   <li>Type: boolean – {@code true} only on the canonical egg</li>
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
     * Hidden UUID component that carries the canonical egg's identity token.
     * The stored value must always match {@link EggPersistentState#getCanonicalEggId()}.
     * It is periodically scrambled to invalidate any copies.
     *
     * <ul>
     *   <li>Namespace: {@code dragonslegacy}</li>
     *   <li>Key: {@code egg_id}</li>
     *   <li>Type: UUID (persisted as int-array)</li>
     *   <li>Only present on the canonical egg; absent on all counterfeits</li>
     * </ul>
     */
    public static final DataComponentType<UUID> EGG_ID = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "egg_id"),
        DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC)
            .build()
    );

    /**
     * Call once during mod initialisation to ensure the component types are
     * registered (triggers static initialisation of this class) and marks the
     * {@code DATA_COMPONENT_TYPE} registry as optional so that vanilla clients
     * are not disconnected during Fabric's registry-sync handshake.
     */
    public static void register() {
        // static initialisers above perform the registrations

        // Mark DATA_COMPONENT_TYPE as optional in Fabric's registry sync so
        // that vanilla (un-modded) clients are not disconnected when they
        // connect to this server.  The component values themselves are already
        // non-synced (no networkSynchronized codec), so vanilla clients never
        // receive the custom component data in item-stack packets.
        RegistryAttributeHolder.get(BuiltInRegistries.DATA_COMPONENT_TYPE)
                .addAttribute(RegistryAttribute.OPTIONAL);
    }

    private EggComponents() {}
}
