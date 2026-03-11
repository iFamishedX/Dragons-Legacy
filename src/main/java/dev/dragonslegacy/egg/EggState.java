package dev.dragonslegacy.egg;

/**
 * Represents the current state / location of the canonical Dragon Egg.
 */
public enum EggState {
    /** The egg is in a (currently online) player's inventory. */
    PLAYER,
    /**
     * The bearer UUID is known but the bearer is currently offline.
     * The egg is safely stored in the offline player's inventory and must
     * NOT trigger "egg dropped" messages, fallback respawns, or UNKNOWN transitions.
     */
    OFFLINE_PLAYER,
    /** The egg is placed as a block in the world. */
    BLOCK,
    /** The egg is a dropped item entity in the world. */
    WORLD,
    /** The egg's location is currently unknown. */
    UNKNOWN
}
