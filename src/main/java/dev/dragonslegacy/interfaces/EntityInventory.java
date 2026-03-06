package dev.dragonslegacy.interfaces;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface EntityInventory {
    SimpleContainer dragonsLegacy$getInventory();

    Vec3 dragonsLegacy$getPos();

    Level dragonsLegacy$getWorld();
}
