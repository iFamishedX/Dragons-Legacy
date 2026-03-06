package dev.dragonslegacy.ability;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.server.level.ServerPlayer;

/**
 * Static utility class that applies and removes all effects associated with the
 * Dragon's Hunger ability.
 *
 * <h3>Effects applied</h3>
 * <ul>
 *   <li>+40 max HP (20 extra hearts) via a transient attribute modifier</li>
 *   <li>Strength II (ambient – does not block other players' potions)</li>
 *   <li>Speed II (ambient – does not block other players' potions)</li>
 *   <li>Hunger II</li>
 *   <li>Curse of Binding on the dragon head in the helmet slot</li>
 * </ul>
 */
public final class DragonHungerAbility {

    /** Identifier for the max-health attribute modifier added by this ability. */
    private static final Identifier HEALTH_MODIFIER_ID =
        Identifier.fromNamespaceAndPath("dragonslegacy", "dragon_hunger_health");

    private DragonHungerAbility() {}

    // -------------------------------------------------------------------------
    // Apply
    // -------------------------------------------------------------------------

    /**
     * Applies all Dragon's Hunger effects to {@code player}.
     *
     * <p>Strength II and Speed II are applied as ambient effects so that potions
     * thrown by other players can visually override them without conflict.
     *
     * @param player the bearer to buff
     */
    public static void apply(ServerPlayer player) {
        applyHealthBoost(player);
        applyEffects(player);
        applyBindingCurse(player);
    }

    // -------------------------------------------------------------------------
    // Remove
    // -------------------------------------------------------------------------

    /**
     * Removes all Dragon's Hunger effects from {@code player}.
     *
     * @param player the bearer to de-buff
     */
    public static void remove(ServerPlayer player) {
        removeHealthBoost(player);
        removeEffects(player);
        removeBindingCurse(player);
    }

    // -------------------------------------------------------------------------
    // Helpers – apply
    // -------------------------------------------------------------------------

    private static void applyHealthBoost(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        // Idempotent: remove first so we don't stack
        attr.removeModifier(HEALTH_MODIFIER_ID);
        attr.addTransientModifier(new AttributeModifier(
            HEALTH_MODIFIER_ID,
            40.0,
            AttributeModifier.Operation.ADD_VALUE
        ));
        // Clamp current HP to new max
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Applies Strength II, Speed II (both ambient), and Hunger II.
     * Ambient effects allow effects from other players' potions to display
     * normally on top of the ability's own effects.
     */
    private static void applyEffects(ServerPlayer player) {
        int duration = AbilityTimers.DEFAULT_DURATION + 20; // slight buffer
        // Strength II – ambient so other players' potions can override visually
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 1, true, false));
        // Speed II – same rationale
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, true, false));
        // Hunger II – visible particles to signal the cost
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, 1, false, true));
    }

    private static void applyBindingCurse(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(Items.DRAGON_HEAD)) return;

        Holder<Enchantment> binding = getBindingCurseHolder(player);
        if (binding == null) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
            head.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
        );
        mutable.set(binding, 1);
        head.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    // -------------------------------------------------------------------------
    // Helpers – remove
    // -------------------------------------------------------------------------

    private static void removeHealthBoost(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(HEALTH_MODIFIER_ID);
        }
        // Clamp current HP so the player doesn't retain phantom HP
        float maxHp = player.getMaxHealth();
        if (player.getHealth() > maxHp) {
            player.setHealth(maxHp);
        }
    }

    private static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.STRENGTH);
        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.HUNGER);
    }

    private static void removeBindingCurse(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return;

        Holder<Enchantment> binding = getBindingCurseHolder(player);
        if (binding == null) return;

        ItemEnchantments existing = head.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (existing.getLevel(binding) <= 0) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
        mutable.set(binding, 0);
        head.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static Holder<Enchantment> getBindingCurseHolder(ServerPlayer player) {
        try {
            return player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BINDING_CURSE);
        } catch (Exception e) {
            return null;
        }
    }
}
