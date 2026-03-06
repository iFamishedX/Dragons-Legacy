package dev.dragonslegacy.egg;

import de.arvitus.dragonegggame.DragonEggGame;
import de.arvitus.dragonegggame.utils.Utils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Prevents the canonical Dragon Egg from being destroyed by:
 * <ul>
 *   <li>explosions</li>
 *   <li>lava / fire</li>
 *   <li>the void</li>
 *   <li>natural item despawn</li>
 * </ul>
 *
 * <p>When any of these conditions are detected, the egg is respawned at the
 * world spawn point via {@link EggSpawnFallback}.
 */
public class EggProtectionManager {

    private final EggSpawnFallback spawnFallback;

    EggProtectionManager(EggSpawnFallback spawnFallback) {
        this.spawnFallback = spawnFallback;
    }

    /**
     * Called when an item entity that is (or contains) a Dragon Egg is about to
     * be removed from the world for a non-player reason (lava, void, explosion, etc.).
     *
     * @param item the item entity that is dying
     */
    public void onEggItemEntityAboutToDie(ItemEntity item) {
        if (!Utils.isOrHasDragonEgg(item.getItem())) return;
        int count = item.getItem().is(Items.DRAGON_EGG)
            ? item.getItem().getCount()
            : Utils.countDragonEgg(item.getItem());

        DragonEggGame.LOGGER.warn(
            "[Dragon's Legacy] Egg item entity is dying unexpectedly – respawning {} egg(s) at spawn.",
            count
        );
        MinecraftServer server = item.level().getServer();
        if (server != null) {
            spawnFallback.ensureEggAtSpawn(server, count);
        }
    }

    /**
     * Called when an egg item entity is about to despawn due to its lifetime
     * exceeding the natural despawn threshold (6000 ticks).
     *
     * @param item the item entity about to despawn
     */
    public void onEggItemDespawn(ItemEntity item) {
        onEggItemEntityAboutToDie(item);
    }

    /**
     * Verifies that a placed Dragon Egg block at the given world/position still
     * exists; if not, triggers the spawn fallback.
     */
    public void protectPlacedEgg(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;
        EggTracker tracker = legacy.getEggTracker();
        if (tracker.getCurrentState() != EggState.PLACED_BLOCK) return;

        var pos = tracker.getPlacedLocation();
        if (pos == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level.getBlockState(pos).is(Blocks.DRAGON_EGG)) return; // still there
        }

        DragonEggGame.LOGGER.warn(
            "[Dragon's Legacy] Placed egg block at {} is gone – respawning at spawn.", pos.toShortString()
        );
        spawnFallback.ensureEggAtSpawn(server, 1);
    }

    /**
     * Full safety check – intended to be called periodically (e.g. every few minutes)
     * to make sure the egg still exists somewhere.
     */
    public void verifyEggSafety(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;
        EggTracker tracker = legacy.getEggTracker();
        EggState state = tracker.getCurrentState();

        if (state == EggState.UNKNOWN) {
            DragonEggGame.LOGGER.warn(
                "[Dragon's Legacy] Egg state is UNKNOWN – triggering spawn fallback."
            );
            spawnFallback.ensureEggAtSpawn(server, 1);
        } else if (state == EggState.PLACED_BLOCK) {
            protectPlacedEgg(server);
        }
    }

    /** Returns the spawn fallback delegate used by this manager. */
    public @Nullable EggSpawnFallback getSpawnFallback() {
        return spawnFallback;
    }
}
