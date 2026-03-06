package dev.dragonslegacy.egg;

import dev.dragonslegacy.utils.Utils;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import dev.dragonslegacy.egg.event.EggBearerChangedEvent;
import dev.dragonslegacy.egg.event.EggDroppedEvent;
import dev.dragonslegacy.egg.event.EggPickedUpEvent;
import dev.dragonslegacy.egg.event.EggPlacedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
/**
 * Tracks the current location and {@link EggState} of the canonical Dragon Egg.
 * Other managers call the {@code updateEgg*} methods to keep this tracker current.
 */
public class EggTracker {

    private EggState currentState = EggState.UNKNOWN;

    /** UUID of the player / entity currently holding the egg, if any. */
    private @Nullable UUID currentBearerUUID;
    /** BlockPos of the placed egg, if any. */
    private @Nullable BlockPos placedLocation;

    private final EggPersistentState persistentState;
    private final DragonEggEventBus eventBus;

    EggTracker(EggPersistentState persistentState, DragonEggEventBus eventBus) {
        this.persistentState = persistentState;
        this.eventBus = eventBus;
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    public EggState getCurrentState() { return currentState; }

    public @Nullable UUID getCurrentBearer() { return currentBearerUUID; }

    public @Nullable ServerPlayer getBearerPlayer(MinecraftServer server) {
        if (currentBearerUUID == null) return null;
        return server.getPlayerList().getPlayer(currentBearerUUID);
    }

    public @Nullable BlockPos getPlacedLocation() { return placedLocation; }

    // -------------------------------------------------------------------------
    // State updates
    // -------------------------------------------------------------------------

    /** Called when a player picks up or holds the canonical egg. */
    public void updateEggHeld(ServerPlayer player) {
        UUID oldBearer = currentBearerUUID;
        currentState      = EggState.HELD_BY_PLAYER;
        currentBearerUUID = player.getUUID();
        placedLocation    = null;

        persistentState.setBearerUUID(player.getUUID());
        persistentState.setBearerLastSeenTick(player.level().getGameTime());

        eventBus.publish(new EggPickedUpEvent(player));
        if (!player.getUUID().equals(oldBearer)) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, player.getUUID()));
        }
    }

    /** Called when the canonical egg is dropped as an item entity. */
    public void updateEggDropped(ItemEntity itemEntity) {
        UUID oldBearer = currentBearerUUID;
        currentState      = EggState.DROPPED_ITEM;
        currentBearerUUID = null;
        placedLocation    = null;

        eventBus.publish(new EggDroppedEvent(itemEntity));
        if (oldBearer != null) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, null));
        }
    }

    /** Called when the canonical egg is placed as a block. */
    public void updateEggPlaced(BlockPos pos) {
        UUID oldBearer = currentBearerUUID;
        currentState      = EggState.PLACED_BLOCK;
        currentBearerUUID = null;
        placedLocation    = pos;

        eventBus.publish(new EggPlacedEvent(pos));
        if (oldBearer != null) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, null));
        }
    }

    /**
     * Scans all online players, block positions, and item entities in all worlds
     * to locate the canonical egg, updating this tracker's state accordingly.
     */
    public void scanAndLocateEgg(MinecraftServer server) {
        // 1 – check player inventories
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Utils.hasDragonEgg(player)) {
                updateEggHeld(player);
                return;
            }
        }

        // 2 – check dropped items in all worlds
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                ItemStack stack = item.getItem();
                if (stack.is(Items.DRAGON_EGG)) {
                    updateEggDropped(item);
                    return;
                }
            }
        }

        // 3 – check a region around spawn for the placed dragon egg block
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getRespawnData().pos();
        for (int dx = -64; dx <= 64; dx++) {
            for (int dz = -64; dz <= 64; dz++) {
                for (int dy = -10; dy <= 10; dy++) {
                    BlockPos candidate = spawn.offset(dx, dy, dz);
                    if (overworld.getBlockState(candidate).is(Blocks.DRAGON_EGG)) {
                        updateEggPlaced(candidate);
                        return;
                    }
                }
            }
        }

        currentState      = EggState.UNKNOWN;
        currentBearerUUID = null;
        placedLocation    = null;
    }
}
