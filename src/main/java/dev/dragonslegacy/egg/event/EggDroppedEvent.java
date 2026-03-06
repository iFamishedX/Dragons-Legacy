package dev.dragonslegacy.egg.event;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * Published when the canonical Dragon Egg is dropped as an item entity.
 */
public final class EggDroppedEvent implements DragonEggEvent {

    private final ItemEntity itemEntity;

    public EggDroppedEvent(ItemEntity itemEntity) {
        this.itemEntity = itemEntity;
    }

    /** The item entity representing the dropped egg. */
    public ItemEntity getItemEntity() { return itemEntity; }
}
