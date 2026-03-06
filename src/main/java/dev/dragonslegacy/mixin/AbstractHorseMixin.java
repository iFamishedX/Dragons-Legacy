package dev.dragonslegacy.mixin;

import dev.dragonslegacy.interfaces.EntityInventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends Animal implements EntityInventory {
    @Shadow
    protected SimpleContainer inventory;

    protected AbstractHorseMixin(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public SimpleContainer dragonsLegacy$getInventory() {
        return this.inventory;
    }

    @Override
    public Vec3 dragonsLegacy$getPos() {
        return this.position();
    }

    @Override
    public Level dragonsLegacy$getWorld() {
        return this.level();
    }
}
