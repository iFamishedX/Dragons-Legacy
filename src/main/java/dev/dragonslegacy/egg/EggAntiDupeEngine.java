package dev.dragonslegacy.egg;

import de.arvitus.dragonegggame.DragonEggGame;
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
 * <p>Priority (highest first):
 * <ol>
 *   <li>Held by a player</li>
 *   <li>Placed block</li>
 *   <li>Dropped item entity</li>
 * </ol>
 */
public class EggAntiDupeEngine {

    private final EggIdentityManager identityManager;

    EggAntiDupeEngine(EggIdentityManager identityManager) {
        this.identityManager = identityManager;
    }

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
        // Collect all canonical egg stacks held by players
        List<ItemStack> playerStacks   = new ArrayList<>();
        List<ItemEntity> droppedItems  = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            collectCanonicalEggs(player.getInventory().items, playerStacks);
            collectCanonicalEggs(player.getInventory().armor, playerStacks);
            collectCanonicalEggs(player.getInventory().offhand, playerStacks);
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                level.getWorldBorder().createBoundingBox())) {
                if (identityManager.isCanonicalEgg(item.getItem())) {
                    droppedItems.add(item);
                }
            }
        }

        int totalCanonical = playerStacks.size() + droppedItems.size();
        if (totalCanonical <= 1) return; // no duplicates

        DragonEggGame.LOGGER.warn(
            "[Dragon's Legacy] Found {} canonical egg(s) – resolving duplicates (keeping 1).",
            totalCanonical
        );

        // Keep the first player stack; remove all others
        boolean kept = false;
        for (ItemStack stack : playerStacks) {
            if (!kept) { kept = true; continue; }
            // Replace with air to effectively destroy the duplicate
            stack.shrink(stack.getCount());
        }

        for (ItemEntity item : droppedItems) {
            if (!kept) { kept = true; continue; }
            item.discard();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void collectCanonicalEggs(List<ItemStack> inventory, List<ItemStack> result) {
        for (ItemStack stack : inventory) {
            if (identityManager.isCanonicalEgg(stack)) result.add(stack);
        }
    }

    /** Checks whether an item entity carries a vanilla Dragon Egg (any count). */
    static boolean isVanillaDragonEgg(ItemEntity item) {
        return item.getItem().is(Items.DRAGON_EGG);
    }
}
