package dev.dragonslegacy.ability.event;

import java.util.UUID;

/**
 * Published when the Dragon's Hunger ability is manually deactivated before its
 * natural expiry (e.g. head removed, bearer status lost).
 */
public final class AbilityDeactivatedEvent implements AbilityEvent {

    private final UUID   playerUUID;
    private final String reason;

    public AbilityDeactivatedEvent(UUID playerUUID, String reason) {
        this.playerUUID = playerUUID;
        this.reason     = reason;
    }

    /** UUID of the player whose ability was deactivated. */
    public UUID getPlayerUUID() { return playerUUID; }

    /** Machine-readable reason string (e.g. {@code "head_removed"}, {@code "lost_bearer_status"}). */
    public String getReason() { return reason; }
}
