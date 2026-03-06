package dev.dragonslegacy.mixin;

import dev.dragonslegacy.interfaces.DoubleInventoryHelper;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Copied from
 * <a href="https://github.com/QuiltServerTools/Ledger/blob/master/src/main/java/com/github/quiltservertools/ledger/mixin/DoubleInventoryMixin.java">ledger</a>
 */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements DoubleInventoryHelper {
    @Shadow
    @Final
    private Container container1;

    @Shadow
    @Final
    private Container container2;

    @NotNull
    @Override
    public Container dragonsLegacy$getInventory(int slot) {
        return slot >= this.container1.getContainerSize() ? this.container2 : this.container1;
    }
}