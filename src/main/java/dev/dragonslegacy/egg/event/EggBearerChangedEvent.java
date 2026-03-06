package dev.dragonslegacy.egg.event;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Published whenever the bearer of the canonical Dragon Egg changes
 * (including when the bearer is cleared, i.e. {@code newBearerUUID} is {@code null}).
 */
public final class EggBearerChangedEvent implements DragonEggEvent {

    private final @Nullable UUID previousBearerUUID;
    private final @Nullable UUID newBearerUUID;

    public EggBearerChangedEvent(@Nullable UUID previousBearerUUID, @Nullable UUID newBearerUUID) {
        this.previousBearerUUID = previousBearerUUID;
        this.newBearerUUID      = newBearerUUID;
    }

    /** UUID of the previous bearer, or {@code null} if there was none. */
    public @Nullable UUID getPreviousBearerUUID() { return previousBearerUUID; }

    /** UUID of the new bearer, or {@code null} if the bearer was cleared. */
    public @Nullable UUID getNewBearerUUID() { return newBearerUUID; }
}
