package dev.dragonslegacy.ability.event;

import java.util.UUID;

/**
 * Published when the Dragon's Hunger ability expires naturally (duration runs out).
 */
public final class AbilityExpiredEvent implements AbilityEvent {

    private final UUID playerUUID;

    public AbilityExpiredEvent(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    /** UUID of the player whose ability expired. */
    public UUID getPlayerUUID() { return playerUUID; }
}
