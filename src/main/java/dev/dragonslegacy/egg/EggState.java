package dev.dragonslegacy.egg;

/**
 * Represents the current state / location of the canonical Dragon Egg.
 */
public enum EggState {
    /** The egg is in a player's inventory. */
    PLAYER,
    /** The egg is placed as a block in the world. */
    BLOCK,
    /** The egg is a dropped item entity in the world. */
    WORLD,
    /** The egg's location is currently unknown. */
    UNKNOWN
}
