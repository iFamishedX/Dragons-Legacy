package dev.dragonslegacy.egg.event;

import net.minecraft.world.phys.Vec3;

/**
 * Published when the canonical Dragon Egg has been teleported (or respawned) at
 * the world spawn point by {@link dev.dragonslegacy.egg.EggSpawnFallback}.
 */
public final class EggTeleportedToSpawnEvent implements DragonEggEvent {

    private final Vec3 spawnPosition;

    public EggTeleportedToSpawnEvent(Vec3 spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    /** The world position at which the egg landed after the teleport. */
    public Vec3 getSpawnPosition() { return spawnPosition; }
}
