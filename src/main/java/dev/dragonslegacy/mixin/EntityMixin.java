package dev.dragonslegacy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "onBelowWorld", at = @At("HEAD"))
    private void dropItemContentsInVoid(CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity item) item.getItem().onDestroyed(item);
    }
}