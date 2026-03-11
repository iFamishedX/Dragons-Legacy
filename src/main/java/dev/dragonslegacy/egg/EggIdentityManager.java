package dev.dragonslegacy.egg;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Assigns and verifies the canonical Dragon Egg identity using the
 * {@code dragonslegacy:egg} boolean data component ({@link EggComponents#EGG}).
 *
 * <p>All detection logic delegates through {@link EggCore#isDragonEgg(ItemStack)}.
 */
public class EggIdentityManager {

    EggIdentityManager() {}

    /**
     * Returns {@code true} if the given stack is the canonical Dragon Egg,
     * i.e. it is a {@link Items#DRAGON_EGG} with {@code dragonslegacy:egg = true}.
     *
     * @deprecated Use {@link EggCore#isDragonEgg(ItemStack)} directly.
     */
    @Deprecated
    public boolean isCanonicalEgg(ItemStack stack) {
        return EggCore.isDragonEgg(stack);
    }

    /**
     * Stamps the given Dragon Egg {@link ItemStack} with the canonical component.
     *
     * @param stack must be a dragon-egg item stack
     * @deprecated Use {@link EggCore#tagEgg(ItemStack)} directly.
     */
    @Deprecated
    public void markAsCanonicalEgg(ItemStack stack) {
        EggCore.tagEgg(stack);
    }
}
