package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Atomic helper for promoting a vanilla Dragon Egg to the canonical egg.
 *
 * <h3>Design contract</h3>
 * <ul>
 *   <li>Promotion must occur <em>only</em> in event handlers, never during periodic
 *       scrub passes or tick callbacks.</li>
 *   <li>All promotion methods are no-ops when a canonical ID already exists.</li>
 *   <li>Each promotion method applies both components ({@code dragonslegacy:egg}
 *       and {@code dragonslegacy:egg_id}) in a single atomic block and immediately
 *       persists the new canonical UUID.</li>
 * </ul>
 */
public final class EggPromotion {

    private EggPromotion() {}

    // -------------------------------------------------------------------------
    // Primary promotion – player inventory
    // -------------------------------------------------------------------------

    /**
     * Scans the given player's full inventory (main + equipment) for a vanilla
     * dragon egg (no Dragon's Legacy components) and, if one is found, promotes
     * it to canonical status atomically.
     *
     * <p>This is a no-op when the canonical ID is already set.
     *
     * @param player the online player whose inventory to scan
     * @return {@code true} if promotion occurred
     */
    public static boolean tryPromote(ServerPlayer player) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return false;
        if (legacy.getPersistentState().getCanonicalEggId() != null) return false;

        // Scan main inventory
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isVanillaDragonEgg(stack)) {
                promoteToCanonical(stack, player);
                return true;
            }
        }
        // Scan equipment slots (hand, armor, offhand)
        for (net.minecraft.world.entity.EquipmentSlot slot
                : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isVanillaDragonEgg(stack)) {
                promoteToCanonical(stack, player);
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Fallback promotion – used at server start and as last resort
    // -------------------------------------------------------------------------

    /**
     * Scans all online players first; if none carry a vanilla dragon egg, falls
     * back to item entities in all loaded levels (last resort).
     *
     * <p>Item entity promotion only happens when:
     * <ol>
     *   <li>{@code canonicalEggId == null}</li>
     *   <li>No online player holds any vanilla dragon egg</li>
     * </ol>
     *
     * @param server the running {@link MinecraftServer}
     * @return {@code true} if promotion occurred
     */
    public static boolean tryPromoteAny(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return false;
        if (legacy.getPersistentState().getCanonicalEggId() != null) return false;

        // Priority 1 – online player inventories
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (tryPromote(player)) return true;
        }

        // Priority 2 – item entities (last resort: only if no player has a vanilla egg)
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            AABB box = new AABB(
                border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                if (isVanillaDragonEgg(item.getItem())) {
                    promoteEntityToCanonical(item);
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Atomic promotion methods
    // -------------------------------------------------------------------------

    /**
     * Atomically promotes the given vanilla dragon egg stack held by a player
     * to canonical status:
     * <ol>
     *   <li>Applies {@code dragonslegacy:egg = true}</li>
     *   <li>Applies {@code dragonslegacy:egg_id = new UUID}</li>
     *   <li>Persists the canonical UUID immediately</li>
     *   <li>Marks the egg as initialised in persistent state</li>
     *   <li>Updates the {@link EggTracker} to {@link EggState#PLAYER}</li>
     *   <li>Logs the promotion</li>
     * </ol>
     *
     * @param stack  a vanilla (untagged) {@link Items#DRAGON_EGG} stack
     * @param player the online player who holds the stack
     */
    public static void promoteToCanonical(ItemStack stack, ServerPlayer player) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return;

        UUID newId = UUID.randomUUID();
        // Apply both components atomically
        stack.set(EggComponents.EGG, true);
        stack.set(EggComponents.EGG_ID, newId);

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy != null) {
            legacy.getPersistentState().setCanonicalEggId(newId);
            legacy.getPersistentState().setEggInitialized(true);
            legacy.getEggTracker().updateEggHeld(player);
        }

        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Canonical Dragon Egg established in inventory of {}.",
            player.getGameProfile().name()
        );
    }

    /**
     * Atomically promotes a vanilla dragon egg item entity to canonical status.
     * This is the <em>last-resort</em> fallback used only when no player holds
     * a vanilla dragon egg.
     *
     * @param item the item entity carrying a vanilla {@link Items#DRAGON_EGG}
     */
    private static void promoteEntityToCanonical(ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!isVanillaDragonEgg(stack)) return;

        UUID newId = UUID.randomUUID();
        stack.set(EggComponents.EGG, true);
        stack.set(EggComponents.EGG_ID, newId);
        item.setItem(stack);

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy != null) {
            legacy.getPersistentState().setCanonicalEggId(newId);
            legacy.getPersistentState().setEggInitialized(true);
            legacy.getEggTracker().updateEggDropped(item);
        }

        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Canonical Dragon Egg established from item entity at {} (fallback).",
            item.blockPosition().toShortString()
        );
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the stack is a plain vanilla Dragon Egg with no
     * Dragon's Legacy components ({@code dragonslegacy:egg} absent and
     * {@code dragonslegacy:egg_id} absent).
     */
    public static boolean isVanillaDragonEgg(ItemStack stack) {
        if (!stack.is(Items.DRAGON_EGG)) return false;
        return !Boolean.TRUE.equals(stack.get(EggComponents.EGG))
            && stack.get(EggComponents.EGG_ID) == null;
    }
}
