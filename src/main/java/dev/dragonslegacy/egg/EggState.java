package dev.dragonslegacy.egg;

/**
 * Represents the current state / location of the canonical Dragon Egg.
 */
public enum EggState {
    /** The egg is in a player's inventory. */
    HELD_BY_PLAYER,
    /** The egg is placed as a block in the world. */
    PLACED_BLOCK,
    /** The egg is a dropped item entity. */
    DROPPED_ITEM,
    /** The egg's location is currently unknown. */
    UNKNOWN
}
