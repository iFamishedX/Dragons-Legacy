package dev.dragonslegacy.ability;

/**
 * Tracks the remaining duration and cooldown ticks for a Dragon's Hunger ability session.
 *
 * <p>Call {@link #tick()} once per server tick while the ability is either
 * {@link AbilityState#ACTIVE} or {@link AbilityState#COOLDOWN}.
 */
public class AbilityTimers {

    /** Total ability duration in ticks (default: 600 ticks = 30 seconds). */
    public static final int DEFAULT_DURATION = 600;

    /** Total cooldown in ticks (default: 6000 ticks = 5 minutes). */
    public static final int DEFAULT_COOLDOWN = 6000;

    private int durationRemaining;
    private int cooldownRemaining;

    /** Creates timers with default duration and zero cooldown. */
    public AbilityTimers() {
        this.durationRemaining = DEFAULT_DURATION;
        this.cooldownRemaining = 0;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    /**
     * Decrements whichever timer is currently relevant.
     * Callers should check {@link #isExpired()} and {@link #isCooldownFinished()} after each tick.
     */
    public void tick() {
        if (durationRemaining > 0) {
            durationRemaining--;
        } else if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns {@code true} when the active duration has reached zero. */
    public boolean isExpired() {
        return durationRemaining <= 0;
    }

    /** Returns {@code true} when the post-expiry cooldown has reached zero. */
    public boolean isCooldownFinished() {
        return cooldownRemaining <= 0;
    }

    // -------------------------------------------------------------------------
    // Control
    // -------------------------------------------------------------------------

    /** Resets duration to {@link #DEFAULT_DURATION} and clears any existing cooldown. */
    public void resetForNewActivation() {
        this.durationRemaining = DEFAULT_DURATION;
        this.cooldownRemaining = 0;
    }

    /** Begins the cooldown phase with {@link #DEFAULT_COOLDOWN} ticks. */
    public void startCooldown() {
        this.durationRemaining = 0;
        this.cooldownRemaining = DEFAULT_COOLDOWN;
    }

    /** Clears any pending cooldown (e.g. when a new bearer takes possession). */
    public void clearCooldown() {
        this.cooldownRemaining = 0;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getDurationRemaining()  { return durationRemaining; }
    public int getCooldownRemaining()  { return cooldownRemaining; }
}
