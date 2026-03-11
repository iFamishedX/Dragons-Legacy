package dev.dragonslegacy.egg;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the canonical Dragon Egg's physical location.
 *
 * @param state     the current {@link EggState}
 * @param pos       block position when state is {@link EggState#BLOCK} or
 *                  {@link EggState#WORLD}; {@code null} for {@link EggState#PLAYER}
 * @param dimension the dimension key; {@code null} when state is {@link EggState#UNKNOWN}
 */
public record EggLocation(
    EggState state,
    @Nullable BlockPos pos,
    @Nullable ResourceKey<Level> dimension
) {}
