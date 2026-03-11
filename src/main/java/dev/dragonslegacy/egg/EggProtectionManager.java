package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
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
 * <p>Egg identity is checked via {@link EggCore#isDragonEgg(net.minecraft.world.item.ItemStack)}.
 * When destruction is detected the egg is respawned via {@link EggSpawnFallback}.
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
        if (!EggCore.isDragonEgg(item.getItem())) return;
        int count = item.getItem().getCount();

        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Canonical egg item entity is dying unexpectedly – respawning {} egg(s) at spawn.",
            count
        );
        MinecraftServer server = item.level().getServer();
        if (server != null) {
            spawnFallback.ensureEggAtSpawn(server, count);
        }
    }

    /**
     * Called when the canonical egg item entity is about to despawn due to its
     * lifetime exceeding the natural despawn threshold (6000 ticks).
     *
     * @param item the item entity about to despawn
     */
    public void onEggItemDespawn(ItemEntity item) {
        onEggItemEntityAboutToDie(item);
    }

    /**
     * Verifies that a placed Dragon Egg block at the tracked position still
     * exists; if not, triggers the spawn fallback.
     */
    public void protectPlacedEgg(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;
        EggTracker tracker = legacy.getEggTracker();
        if (tracker.getCurrentState() != EggState.BLOCK) return;

        var pos = tracker.getPlacedLocation();
        if (pos == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (level.getBlockState(pos).is(Blocks.DRAGON_EGG)) return; // still there
        }

        DragonsLegacyMod.LOGGER.warn(
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
        if (!legacy.getPersistentState().isEggInitialized()) return;
        EggTracker tracker = legacy.getEggTracker();
        EggState state = tracker.getCurrentState();

        if (state == EggState.UNKNOWN) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Egg state is UNKNOWN – triggering spawn fallback."
            );
            spawnFallback.ensureEggAtSpawn(server, 1);
        } else if (state == EggState.BLOCK) {
            protectPlacedEgg(server);
        }
    }

    /** Returns the spawn fallback delegate used by this manager. */
    public @Nullable EggSpawnFallback getSpawnFallback() {
        return spawnFallback;
    }
}
