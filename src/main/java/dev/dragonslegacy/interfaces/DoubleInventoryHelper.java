package dev.dragonslegacy.interfaces;


import net.minecraft.world.Container;

/**
 * Copied from
 * <a href="https://github.com/QuiltServerTools/Ledger/blob/master/src/main/kotlin/com/github/quiltservertools/ledger/actionutils/DoubleInventoryHelper.kt">ledger</a>
 */
public interface DoubleInventoryHelper {
    Container dragonsLegacy$getInventory(int slot);
}