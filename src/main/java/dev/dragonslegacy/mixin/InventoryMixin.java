package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inspired by
 * <a href="https://github.com/QuiltServerTools/Ledger/blob/master/src/main/java/com/github/quiltservertools/ledger/mixin/LockableContainerBlockEntityMixin.java">ledger</a>
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin implements Container {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void onItemInsertion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemStack = stack.copyWithCount(cir.getReturnValue() ? 1 : 0);
        if (Utils.isOrHasDragonEgg(itemStack)) {
            DragonEggAPI.updatePosition(this.player);
            // Request an immediate scrub so the anti-dupe engine sees the egg
            // in this player's inventory before promoting any item entity
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy != null) legacy.getEggCore().requestImmediateScrub();
        }
    }
}