package dev.dragonslegacy.ability.event;

import java.util.UUID;

/**
 * Published when the Dragon's Hunger ability is successfully activated for a player.
 */
public final class AbilityActivatedEvent implements AbilityEvent {

    private final UUID playerUUID;
    private final int  duration;

    public AbilityActivatedEvent(UUID playerUUID, int duration) {
        this.playerUUID = playerUUID;
        this.duration   = duration;
    }

    /** UUID of the player who activated the ability. */
    public UUID getPlayerUUID() { return playerUUID; }

    /** The total duration of the ability in ticks. */
    public int getDuration() { return duration; }
}
