package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    protected ServerLevelMixin(
        WritableLevelData properties,
        ResourceKey<Level> registryRef,
        RegistryAccess registryManager,
        Holder<DimensionType> dimensionEntry,
        boolean isClient,
        boolean debugWorld,
        long biomeAccess,
        int maxChainedNeighborUpdates
    ) {
        super(
            properties,
            registryRef,
            registryManager,
            dimensionEntry,
            isClient,
            debugWorld,
            biomeAccess,
            maxChainedNeighborUpdates
        );
    }

    @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"))
    public void onBlockStateChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        // Block entities are not yet loaded at this point
        if (newBlock.is(Blocks.DRAGON_EGG)) DragonEggAPI.updatePosition(pos, this);
    }
}
