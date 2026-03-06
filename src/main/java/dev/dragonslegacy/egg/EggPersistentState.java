package dev.dragonslegacy.egg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
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

    private static final String KEY_EGG_ID          = "canonical_egg_id";
    private static final String KEY_BEARER_UUID      = "bearer_uuid";
    private static final String KEY_BEARER_LAST_SEEN = "bearer_last_seen";

    private @Nullable UUID canonicalEggId;
    private @Nullable UUID bearerUUID;
    /** World game-time tick at which the bearer was last seen online. */
    private long bearerLastSeenTick = -1L;

    public EggPersistentState() {}

    // -------------------------------------------------------------------------
    // SavedDataType / serialisation
    // -------------------------------------------------------------------------

    /** Type registration used by {@link net.minecraft.world.level.storage.DimensionDataStorage}. */
    public static final SavedDataType<EggPersistentState> TYPE = new SavedDataType<>(
        "dragons_legacy_egg",
        ctx -> new EggPersistentState(),
        ctx -> RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf(KEY_EGG_ID)
                .forGetter(s -> Optional.ofNullable(s.canonicalEggId)),
            UUIDUtil.CODEC.optionalFieldOf(KEY_BEARER_UUID)
                .forGetter(s -> Optional.ofNullable(s.bearerUUID)),
            Codec.LONG.optionalFieldOf(KEY_BEARER_LAST_SEEN, -1L)
                .forGetter(s -> s.bearerLastSeenTick)
        ).apply(instance, EggPersistentState::create))
    );

    private static EggPersistentState create(
            Optional<UUID> canonicalEggId,
            Optional<UUID> bearerUUID,
            long bearerLastSeenTick) {
        EggPersistentState state = new EggPersistentState();
        state.canonicalEggId    = canonicalEggId.orElse(null);
        state.bearerUUID        = bearerUUID.orElse(null);
        state.bearerLastSeenTick = bearerLastSeenTick;
        return state;
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
