package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import dev.dragonslegacy.egg.event.EggBearerChangedEvent;
import dev.dragonslegacy.egg.event.EggDroppedEvent;
import dev.dragonslegacy.egg.event.EggPickedUpEvent;
import dev.dragonslegacy.egg.event.EggPlacedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Tracks the current location and {@link EggState} of the canonical Dragon Egg.
 * Other managers call the {@code updateEgg*} methods to keep this tracker current.
 *
 * <p>Egg identity is determined solely by the {@code dragonslegacy:egg} component
 * via {@link EggCore#isDragonEgg(ItemStack)}.
 */
public class EggTracker {

    private EggState currentState = EggState.UNKNOWN;

    /** UUID of the player currently holding the egg, if any. */
    private @Nullable UUID currentBearerUUID;
    /** BlockPos of the placed egg, if any. */
    private @Nullable BlockPos placedLocation;
    /** Dimension of the egg when it is placed (BLOCK) or dropped (WORLD). */
    private @Nullable ResourceKey<Level> eggDimension;

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

    public @Nullable ResourceKey<Level> getEggDimension() { return eggDimension; }

    // -------------------------------------------------------------------------
    // State updates
    // -------------------------------------------------------------------------

    /** Called when a player picks up or holds the canonical egg. */
    public void updateEggHeld(ServerPlayer player) {
        UUID oldBearer = currentBearerUUID;
        EggState oldState = currentState;
        currentState      = EggState.PLAYER;
        currentBearerUUID = player.getUUID();
        placedLocation    = null;
        eggDimension      = null;

        persistentState.setBearerUUID(player.getUUID());
        persistentState.setBearerLastSeenTick(player.level().getGameTime());
        persistentState.setEggInitialized(true);

        // Only fire the pickup announcement when the state actually transitions
        // (not on every periodic re-scan while the player already holds the egg).
        if (oldState != EggState.PLAYER || !player.getUUID().equals(oldBearer)) {
            eventBus.publish(new EggPickedUpEvent(player));
        }
        if (!player.getUUID().equals(oldBearer)) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, player.getUUID()));
        }
    }

    /** Called when the canonical egg is dropped as an item entity. */
    public void updateEggDropped(ItemEntity itemEntity) {
        UUID oldBearer = currentBearerUUID;
        EggState oldState = currentState;
        currentState      = EggState.WORLD;
        currentBearerUUID = null;
        placedLocation    = itemEntity.blockPosition();
        eggDimension      = itemEntity.level().dimension();

        // Only fire the "dropped" announcement when the state actually transitions
        // to WORLD – not on every periodic re-scan while the egg lies on the ground.
        if (oldState != EggState.WORLD) {
            eventBus.publish(new EggDroppedEvent(itemEntity));
        }
        if (oldBearer != null) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, null));
        }
    }

    /** Called when the canonical egg is placed as a block. */
    public void updateEggPlaced(BlockPos pos, ResourceKey<Level> dimension) {
        UUID oldBearer = currentBearerUUID;
        EggState oldState = currentState;
        currentState      = EggState.BLOCK;
        currentBearerUUID = null;
        placedLocation    = pos;
        eggDimension      = dimension;

        persistentState.setEggInitialized(true);

        // Only fire the placement announcement on actual state transition.
        if (oldState != EggState.BLOCK) {
            eventBus.publish(new EggPlacedEvent(pos));
        }
        if (oldBearer != null) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, null));
        }
    }

    /** Clears the bearer without changing the egg's world position or state. */
    public void clearBearer() {
        UUID oldBearer = currentBearerUUID;
        currentBearerUUID = null;
        persistentState.setBearerUUID(null);
        if (oldBearer != null) {
            eventBus.publish(new EggBearerChangedEvent(oldBearer, null));
        }
    }

    /** Clears the stored placed-block location. */
    public void clearPlacedLocation() {
        placedLocation = null;
        eggDimension   = null;
    }

    /**
     * Called when the bearer is known but currently offline.
     * Transitions state to {@link EggState#OFFLINE_PLAYER} and preserves the
     * bearer UUID so the system can resume when they reconnect.
     */
    public void updateEggOfflinePlayer(UUID bearerUuid) {
        currentState      = EggState.OFFLINE_PLAYER;
        currentBearerUUID = bearerUuid;
        placedLocation    = null;
        eggDimension      = null;
        persistentState.setBearerUUID(bearerUuid);
    }

    // -------------------------------------------------------------------------
    // Scan
    // -------------------------------------------------------------------------

    /**
     * Scans all online players, item entities in all worlds, and the known block
     * position to locate the canonical egg, updating this tracker's state.
     *
     * <p>Detection order (highest priority first):
     * <ol>
     *   <li>Player inventories (main, equipment, offhand)</li>
     *   <li>Dropped item entities (all loaded levels)</li>
     *   <li>Placed block at the tracked position</li>
     * </ol>
     *
     * <p><strong>Migration:</strong> If no tagged egg is found but a bare
     * {@link Items#DRAGON_EGG} exists and the egg has been initialised in this
     * world, it is automatically tagged with the component and adopted.
     */
    public void scanAndLocateEgg(MinecraftServer server) {
        // Pre-check: if bearer UUID is known and bearer is offline, the egg is safely
        // stored in the offline player's inventory.  Return early to avoid false
        // "egg dropped" messages, UNKNOWN transitions, and fallback respawns.
        if (currentBearerUUID != null) {
            ServerPlayer bearer = server.getPlayerList().getPlayer(currentBearerUUID);
            if (bearer == null) {
                // Bearer is offline – update state without changing the already-stored UUID.
                currentState   = EggState.OFFLINE_PLAYER;
                placedLocation = null;
                eggDimension   = null;
                return;
            }
        }

        // 1 – player inventories
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack found = findTaggedEggInPlayer(player);
            if (found != null) {
                updateEggHeld(player);
                return;
            }
        }

        // 2 – dropped item entities in all loaded levels
        UUID canonicalId = persistentState.getCanonicalEggId();
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                border.getMinX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                ItemStack stack = item.getItem();
                if (isTrackerCanonical(stack, canonicalId)) {
                    updateEggDropped(item);
                    return;
                }
            }
        }

        // 3 – placed block at tracked position
        if (placedLocation != null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getBlockState(placedLocation).is(Blocks.DRAGON_EGG)) {
                    // state remains BLOCK – just confirm it
                    currentState = EggState.BLOCK;
                    eggDimension = level.dimension();
                    return;
                }
            }
            // Block is gone
            clearPlacedLocation();
        }

        // --- Nothing found via component – attempt migration for old worlds ---
        if (persistentState.isEggInitialized()) {
            if (migrateUntaggedEgg(server)) return;
        }

        currentState      = EggState.UNKNOWN;
        currentBearerUUID = null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Searches the player's main inventory, equipment, and container-cursor for
     * a tagged Dragon Egg with the correct canonical UUID.
     *
     * <p>Accepts eggs with {@code dragonslegacy:egg = true} but no {@code egg_id}
     * only when no canonical ID is stored yet, or when the canonical ID matches
     * the stored value (migration case: egg was tagged before UUID system).
     *
     * @return the canonical stack if found, {@code null} otherwise
     */
    private @Nullable ItemStack findTaggedEggInPlayer(ServerPlayer player) {
        UUID canonicalId = persistentState.getCanonicalEggId();
        // Main inventory
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isTrackerCanonical(stack, canonicalId)) return stack;
        }
        // Equipment (armour + offhand)
        for (net.minecraft.world.entity.EquipmentSlot slot
                : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isTrackerCanonical(stack, canonicalId)) return stack;
        }
        return null;
    }

    /**
     * Returns {@code true} when the stack should be treated as the canonical egg
     * by the tracker.  This is true when:
     * <ul>
     *   <li>The stack fully matches the canonical UUID ({@link EggCore#isCanonicalEgg})</li>
     *   <li>OR the stack has {@code dragonslegacy:egg = true} but no UUID yet
     *       (legacy / migration: the scrub pass will assign the UUID)</li>
     * </ul>
     */
    private static boolean isTrackerCanonical(ItemStack stack, @Nullable UUID canonicalId) {
        if (!EggCore.isDragonEgg(stack)) return false;
        UUID stackId = stack.get(EggComponents.EGG_ID);
        if (stackId == null) {
            // Legacy / migration: egg tagged but UUID not yet set – accept it so
            // the scrub pass can promote/migrate it rather than losing track.
            return true;
        }
        return stackId.equals(canonicalId);
    }

    /**
     * Migration helper: if no tagged egg was found, look for any bare
     * {@link Items#DRAGON_EGG} and tag it as canonical (old-world migration).
     *
     * @return {@code true} if a bare egg was found and adopted
     */
    private boolean migrateUntaggedEgg(MinecraftServer server) {
        // Check player inventories first
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.is(Items.DRAGON_EGG) && !EggCore.isDragonEgg(stack)) {
                    EggCore.tagEgg(stack);
                    updateEggHeld(player);
                    DragonsLegacyMod.LOGGER.info(
                        "[Dragon's Legacy] Migrated untagged egg in {}'s inventory to component-based identity.",
                        player.getGameProfile().name());
                    return true;
                }
            }
        }
        // Check item entities
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                border.getMinX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                ItemStack stack = item.getItem();
                if (stack.is(Items.DRAGON_EGG) && !EggCore.isDragonEgg(stack)) {
                    EggCore.tagEgg(stack);
                    item.setItem(stack);
                    updateEggDropped(item);
                    DragonsLegacyMod.LOGGER.info(
                        "[Dragon's Legacy] Migrated untagged egg item entity to component-based identity.");
                    return true;
                }
            }
        }
        // Check known placed position (scan wider area around spawn)
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getRespawnData().pos();
        for (int dx = -64; dx <= 64; dx++) {
            for (int dz = -64; dz <= 64; dz++) {
                for (int dy = -10; dy <= 10; dy++) {
                    BlockPos candidate = spawn.offset(dx, dy, dz);
                    if (overworld.getBlockState(candidate).is(Blocks.DRAGON_EGG)) {
                        updateEggPlaced(candidate, overworld.dimension());
                        DragonsLegacyMod.LOGGER.info(
                            "[Dragon's Legacy] Migrated placed egg block at {} to component-based identity.",
                            candidate.toShortString());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
