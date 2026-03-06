package dev.dragonslegacy.egg;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Minecraft {@link SavedData} that persists Dragon's Legacy state between restarts:
 * <ul>
 *   <li>canonical egg UUID</li>
 *   <li>current bearer UUID</li>
 *   <li>last time the bearer was online (world game-time ticks)</li>
 * </ul>
 */
public class EggPersistentState extends SavedData {

    public static final String SAVE_ID = "dragons_legacy_egg";

    private static final String KEY_EGG_ID          = "canonical_egg_id";
    private static final String KEY_BEARER_UUID      = "bearer_uuid";
    private static final String KEY_BEARER_LAST_SEEN = "bearer_last_seen";

    private @Nullable UUID canonicalEggId;
    private @Nullable UUID bearerUUID;
    /** World game-time tick at which the bearer was last seen online. */
    private long bearerLastSeenTick = -1L;

    public EggPersistentState() {}

    // -------------------------------------------------------------------------
    // Factory / serialisation
    // -------------------------------------------------------------------------

    /** Factory used by {@link net.minecraft.world.level.storage.DimensionDataStorage}. */
    public static final SavedData.Factory<EggPersistentState> FACTORY =
        new SavedData.Factory<>(EggPersistentState::new, EggPersistentState::fromTag);

    /** Deserialises from NBT (called by the SavedData infrastructure). */
    public static EggPersistentState fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        EggPersistentState state = new EggPersistentState();
        if (tag.contains(KEY_EGG_ID)) {
            state.canonicalEggId = tag.getUUID(KEY_EGG_ID);
        }
        if (tag.contains(KEY_BEARER_UUID)) {
            state.bearerUUID = tag.getUUID(KEY_BEARER_UUID);
        }
        state.bearerLastSeenTick = tag.getLong(KEY_BEARER_LAST_SEEN);
        return state;
    }

    /** Serialises to NBT. */
    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (canonicalEggId != null)    tag.putUUID(KEY_EGG_ID, canonicalEggId);
        if (bearerUUID != null)        tag.putUUID(KEY_BEARER_UUID, bearerUUID);
        tag.putLong(KEY_BEARER_LAST_SEEN, bearerLastSeenTick);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public @Nullable UUID getCanonicalEggId() { return canonicalEggId; }

    public void setCanonicalEggId(@Nullable UUID id) {
        this.canonicalEggId = id;
        setDirty();
    }

    public @Nullable UUID getBearerUUID() { return bearerUUID; }

    public void setBearerUUID(@Nullable UUID uuid) {
        this.bearerUUID = uuid;
        setDirty();
    }

    public long getBearerLastSeenTick() { return bearerLastSeenTick; }

    public void setBearerLastSeenTick(long tick) {
        this.bearerLastSeenTick = tick;
        setDirty();
    }
}
