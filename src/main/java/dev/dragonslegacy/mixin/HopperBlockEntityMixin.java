package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(
        method = "tryMoveInItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V", shift = At.Shift.AFTER)
    )
    private static void transfer(
        @Nullable Container from,
        Container to,
        ItemStack stack,
        int slot,
        @Nullable Direction side,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (Utils.isOrHasDragonEgg(to.getItem(slot)) && to instanceof BlockEntity blockEntity) {
            DragonEggAPI.updatePosition(blockEntity);
        }
    }
}
