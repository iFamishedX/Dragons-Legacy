package dev.dragonslegacy.egg;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Assigns and verifies the canonical Dragon Egg identity using a UUID stored
 * in the item's {@code CustomData} NBT under the key {@code "dragons_legacy_egg_id"}.
 */
public class EggIdentityManager {

    private static final String NBT_KEY = "dragons_legacy_egg_id";

    private @Nullable UUID canonicalEggId;

    EggIdentityManager() {}

    /**
     * Returns the canonical egg UUID, or {@code null} if not yet assigned.
     */
    public @Nullable UUID getCanonicalEggId() {
        return canonicalEggId;
    }

    /** Sets the canonical egg UUID (called when loading or first assigning). */
    void setCanonicalEggId(@Nullable UUID id) {
        this.canonicalEggId = id;
    }

    /**
     * Returns {@code true} if the given stack is the canonical Dragon Egg
     * (i.e. it carries the canonical UUID in its custom data).
     */
    public boolean isCanonicalEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return false;
        UUID id = getEggIdFromStack(stack);
        return id != null && id.equals(canonicalEggId);
    }

    /**
     * Stamps the given Dragon Egg {@link ItemStack} with the canonical UUID,
     * creating a new UUID if none has been assigned yet.
     *
     * @param stack must be a dragon-egg item stack
     */
    public void markAsCanonicalEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return;

        if (canonicalEggId == null) {
            canonicalEggId = UUID.randomUUID();
        }

        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = existing.copyTag();
        tag.putString(NBT_KEY, canonicalEggId.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Reads the Dragon's-Legacy egg UUID embedded in the given stack, or {@code null}
     * if the stack does not carry one.
     */
    public @Nullable UUID getEggIdFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        try {
            return tag.getString(NBT_KEY)
                .map(s -> { try { return UUID.fromString(s); } catch (Exception e) { return null; } })
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
