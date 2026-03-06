package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.interfaces.BlockInventory;
import dev.dragonslegacy.interfaces.DoubleInventoryHelper;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inspired by
 * <a href="https://github.com/QuiltServerTools/Ledger/blob/master/src/main/java/com/github/quiltservertools/ledger/mixin/SlotMixin.java">ledger</a>
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow
    @Final
    public Container container;
    @Shadow
    @Final
    private int slot;

    @Inject(method = "setByPlayer(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void onStackChange(ItemStack stack, CallbackInfo ci) {
        if (!Utils.isOrHasDragonEgg(stack)) return;

        switch (this.container) {
            case Inventory playerInventory -> DragonEggAPI.updatePosition(playerInventory.player);
            case BlockEntity blockEntity -> DragonEggAPI.updatePosition(blockEntity);
            case Entity entity -> DragonEggAPI.updatePosition(entity);
            default -> {
                BlockInventory blockInventory = this.getBlockInventory();
                if (blockInventory == null) return;
                BlockPos pos = blockInventory.dragonsLegacy$getPos();
                Level world = blockInventory.dragonsLegacy$getWorld();
                if (pos == null || world == null) return;
                DragonEggAPI.updatePosition(DragonEggAPI.PositionType.INVENTORY, pos, world);
            }
        }
    }

    @Unique
    @Nullable
    private BlockInventory getBlockInventory() {
        Container slotInventory = this.container;
        if (slotInventory instanceof DoubleInventoryHelper doubleInventoryHelper) {
            slotInventory = doubleInventoryHelper.dragonsLegacy$getInventory(this.slot);
        }
        if (slotInventory instanceof BlockInventory blockInventory) {
            return blockInventory;
        }

        return null;
    }
}
