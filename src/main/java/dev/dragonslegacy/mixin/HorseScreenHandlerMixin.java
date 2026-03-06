package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMountInventoryMenu.class)
public abstract class HorseScreenHandlerMixin {
    @Shadow
    @Final
    protected LivingEntity mount;

    @Inject(method = "removed", at = @At("HEAD"))
    public void onClosed(Player player, CallbackInfo ci) {
        if (Utils.hasDragonEgg(this.mount)) DragonEggAPI.updatePosition(this.mount);
    }
}
