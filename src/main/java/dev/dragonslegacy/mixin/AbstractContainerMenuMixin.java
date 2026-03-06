package dev.dragonslegacy.mixin;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow
    @Final
    public NonNullList<@NotNull Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void blockDragonEggInsertion(int i, int j, ClickType clickType, Player player, CallbackInfo ci) {
        if (i < 0) return;

        var isEnderChestMenu = slots.stream().anyMatch(s -> s.container instanceof PlayerEnderChestContainer);
        if (isEnderChestMenu && DragonsLegacyMod.CONFIG.blockEnderChest && checkDragonEgg(i, j, clickType, player))
            ci.cancel();
    }

    @Unique
    private boolean checkDragonEgg(int i, int j, ClickType clickType, Player player) {
        var slot = slots.get(i);
        var playerInventory = player.getInventory();
        var isPlayerSlot = slot.container == playerInventory;

        var stack = switch (clickType) {
            case QUICK_MOVE -> isPlayerSlot ? slot.getItem() : ItemStack.EMPTY;
            case SWAP -> isPlayerSlot ? ItemStack.EMPTY : playerInventory.getItem(j);
            default -> isPlayerSlot ? ItemStack.EMPTY : getCarried();
        };

        // recursive bundle handling
        // disabled because it's too specific and not needed to properly track the egg
        // if (clickType == ClickType.PICKUP) {
        //     var carriedIsBundle = getCarried().getItem() instanceof BundleItem;
        //     var slotIsBundle = slot.hasItem() && slot.getItem().getItem() instanceof BundleItem;
        //
        //     if (j == 0) { // 0 = left click, 1 = right click
        //         if (carriedIsBundle && slot.hasItem() && DragonsLegacyMod.CONFIG.blockContainerItems)
        //             stack = slot.getItem();
        //         else if (!isPlayerSlot || slotIsBundle && DragonsLegacyMod.CONFIG.blockContainerItems)
        //             stack = getCarried();
        //         else return false;
        //     } else {
        //         if (isPlayerSlot) return false;
        //
        //         if (slot.hasItem() || !carriedIsBundle) stack = getCarried();
        //         else {
        //             var bundleContents = getCarried().get(DataComponents.BUNDLE_CONTENTS);
        //             return bundleContents != null
        //                    && !bundleContents.isEmpty()
        //                    && Utils.isOrHasDragonEgg(bundleContents.getItemUnsafe(0));
        //         }
        //     }
        // }

        return Utils.isOrHasDragonEgg(stack);
    }
}
