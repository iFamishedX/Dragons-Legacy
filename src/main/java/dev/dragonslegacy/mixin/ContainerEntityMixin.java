package dev.dragonslegacy.mixin;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerEntity.class)
public interface ContainerEntityMixin {
    @Inject(method = "setChestVehicleItem", at = @At("HEAD"))
    default void onSetInventoryStack(int slot, ItemStack stack, CallbackInfo ci) {
        if (this instanceof Entity entity && Utils.isOrHasDragonEgg(stack)) DragonEggAPI.updatePosition(entity);
    }
}
