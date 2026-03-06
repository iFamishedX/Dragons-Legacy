package dev.dragonslegacy.egg.event;

import net.minecraft.core.BlockPos;

/**
 * Published when the canonical Dragon Egg is placed as a block in the world.
 */
public final class EggPlacedEvent implements DragonEggEvent {

    private final BlockPos position;

    public EggPlacedEvent(BlockPos position) {
        this.position = position;
    }

    /** The world position at which the egg block was placed. */
    public BlockPos getPosition() { return position; }
}
