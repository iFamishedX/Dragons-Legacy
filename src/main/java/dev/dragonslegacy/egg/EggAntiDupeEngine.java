package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects and removes duplicate canonical Dragon Eggs so only one copy
 * can ever exist in the world at any given time.
 *
 * <p>Detection relies on {@link EggCore#isDragonEgg(ItemStack)} – the
 * {@code dragonslegacy:egg} component is the sole identity signal.
 *
 * <p>Priority (highest first):
 * <ol>
 *   <li>Held by a player</li>
 *   <li>Dropped item entity</li>
 * </ol>
 */
public class EggAntiDupeEngine {

    EggAntiDupeEngine() {}

    /**
     * Entry point: scans for duplicates and resolves them.
     */
    public void scanAndResolveDuplicates(MinecraftServer server) {
        resolveDuplicateCanonicalEggs(server);
    }

    /**
     * Finds every canonical Dragon Egg across all players and item entities,
     * keeps the highest-priority one and removes all others.
     */
    public void resolveDuplicateCanonicalEggs(MinecraftServer server) {
        List<ItemStack>  playerStacks  = new ArrayList<>();
        List<ItemEntity> droppedItems  = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (EggCore.isDragonEgg(stack)) playerStacks.add(stack);
            }
            for (net.minecraft.world.entity.EquipmentSlot slot
                    : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (EggCore.isDragonEgg(stack)) playerStacks.add(stack);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                if (EggCore.isDragonEgg(item.getItem())) {
                    droppedItems.add(item);
                }
            }
        }

        int totalCanonical = playerStacks.size() + droppedItems.size();
        if (totalCanonical <= 1) return;

        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Found {} canonical egg(s) – removing duplicates (keeping 1).",
            totalCanonical
        );

        // Keep the first player stack; remove all others
        boolean kept = false;
        for (ItemStack stack : playerStacks) {
            if (!kept) { kept = true; continue; }
            stack.shrink(stack.getCount());
        }
        for (ItemEntity item : droppedItems) {
            if (!kept) { kept = true; continue; }
            item.discard();
        }
    }

    /** Checks whether an item entity carries a vanilla Dragon Egg (any count). */
    static boolean isVanillaDragonEgg(ItemEntity item) {
        return item.getItem().is(Items.DRAGON_EGG);
    }
}
