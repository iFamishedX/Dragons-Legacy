package dev.dragonslegacy.ability;

/**
 * Represents the lifecycle state of the Dragon's Hunger ability for a specific player.
 */
public enum AbilityState {

    /** No ability is running and no cooldown is in effect. */
    INACTIVE,

    /** The ability is currently active and affecting the player. */
    ACTIVE,

    /** The ability has expired or been deactivated and is cooling down. */
    COOLDOWN
}
