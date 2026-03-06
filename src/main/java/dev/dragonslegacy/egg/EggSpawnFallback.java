package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import dev.dragonslegacy.egg.event.EggTeleportedToSpawnEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Ensures the canonical Dragon Egg is always recoverable by teleporting it
 * (or spawning a fresh one) to the world spawn point when it would otherwise
 * be lost.
 */
public class EggSpawnFallback {

    private final DragonEggEventBus eventBus;
    private boolean enabled = true;

    EggSpawnFallback(DragonEggEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /** Enables or disables the spawn-fallback behaviour. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Returns {@code true} if spawn-fallback is currently enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks whether the canonical egg exists anywhere. If not, spawns one at the
     * overworld spawn point.  Does nothing when {@link #isEnabled()} is {@code false}.
     */
    public void ensureEggExists(MinecraftServer server) {
        if (!enabled) return;
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;
        EggTracker tracker = legacy.getEggTracker();
        if (tracker.getCurrentState() != EggState.UNKNOWN) return;
        ensureEggAtSpawn(server, 1);
    }

    /**
     * If the canonical egg cannot be found anywhere, spawns {@code count}
     * dragon eggs at the overworld spawn.
     */
    public void ensureEggAtSpawn(MinecraftServer server, int count) {
        ItemEntity spawned = Utils.spawnDragonEggAtSpawn(server, count);
        if (spawned != null) {
            spawned.setGlowingTag(true);
            Vec3 pos = spawned.position();
            eventBus.publish(new EggTeleportedToSpawnEvent(pos));
            DragonsLegacyMod.LOGGER.info(
                "[Dragon's Legacy] Spawned {} egg(s) at spawn ({}).", count, pos
            );
        } else {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Could not spawn egg at spawn – world may not be ready."
            );
        }
    }

    /**
     * Teleports an existing item entity or placed block egg to the spawn point.
     * If no existing egg is found, falls back to {@link #ensureEggAtSpawn}.
     */
    public void teleportEggToSpawn(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        Vec3 spawnPos = overworld.getRespawnData().pos().getCenter();

        // Try to move an existing item entity
        @Nullable ItemEntity existing = findEggItemEntity(server);
        if (existing != null) {
            existing.teleportTo(spawnPos.x, spawnPos.y, spawnPos.z);
            existing.setGlowingTag(true);
            eventBus.publish(new EggTeleportedToSpawnEvent(spawnPos));
            DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Teleported egg item entity to spawn.");
            return;
        }

        // Try to remove a placed block and spawn the item at spawn
        @Nullable var placedPos = findPlacedEggBlock(server);
        if (placedPos != null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getBlockState(placedPos).is(Blocks.DRAGON_EGG)) {
                    level.removeBlock(placedPos, false);
                    break;
                }
            }
            ensureEggAtSpawn(server, 1);
            return;
        }

        // No egg found at all – spawn fresh
        ensureEggAtSpawn(server, 1);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private @Nullable ItemEntity findEggItemEntity(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                if (item.getItem().is(Items.DRAGON_EGG)) return item;
            }
        }
        return null;
    }

    private @Nullable net.minecraft.core.BlockPos findPlacedEggBlock(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return null;
        EggTracker tracker = legacy.getEggTracker();
        if (tracker.getCurrentState() == EggState.PLACED_BLOCK) {
            return tracker.getPlacedLocation();
        }
        return null;
    }
}
