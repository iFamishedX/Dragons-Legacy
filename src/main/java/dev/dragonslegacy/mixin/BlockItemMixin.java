package dev.dragonslegacy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dragonslegacy.DragonsLegacyMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    public abstract Block getBlock();

    @ModifyReturnValue(method = "canFitInsideContainerItems", at = @At("RETURN"))
    private boolean blockDragonEggInsertion(boolean original) {
        if (!DragonsLegacyMod.CONFIG.blockContainerItems) return original;
        return original && getBlock() != Blocks.DRAGON_EGG;
    }
}
