package dev.dragonslegacy.ability;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.ability.event.AbilityActivatedEvent;
import dev.dragonslegacy.ability.event.AbilityDeactivatedEvent;
import dev.dragonslegacy.ability.event.AbilityExpiredEvent;
import dev.dragonslegacy.egg.DragonsLegacy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Central manager for the Dragon's Hunger ability.
 *
 * <p>Tracks the active ability state, remaining duration, and cooldown timer
 * for the current bearer. Publishes {@link AbilityActivatedEvent},
 * {@link AbilityExpiredEvent}, and {@link AbilityDeactivatedEvent} on the
 * shared {@link dev.dragonslegacy.egg.event.DragonEggEventBus}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Call {@link #init(MinecraftServer)} once when the server starts.</li>
 *   <li>Call {@link #tick(MinecraftServer)} on every server tick.</li>
 * </ol>
 */
public class AbilityEngine {

    private AbilityState          state   = AbilityState.INACTIVE;
    private final AbilityTimers   timers  = new AbilityTimers();
    private @Nullable UUID        activePlayerUUID;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises the engine. Currently a no-op but provided for consistency
     * with other Dragon's Legacy sub-managers.
     *
     * @param server the running {@link MinecraftServer}
     */
    public void init(MinecraftServer server) {
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] AbilityEngine initialised.");
    }

    /**
     * Called every server tick.  Handles duration countdown and automatic expiry.
     *
     * @param server the running {@link MinecraftServer}
     */
    public void tick(MinecraftServer server) {
        if (state == AbilityState.INACTIVE) return;

        timers.tick();

        if (state == AbilityState.ACTIVE && timers.isExpired()) {
            expireAbility(server);
        } else if (state == AbilityState.COOLDOWN && timers.isCooldownFinished()) {
            state = AbilityState.INACTIVE;
            DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Dragon's Hunger cooldown finished.");
        }
    }

    // -------------------------------------------------------------------------
    // Activation
    // -------------------------------------------------------------------------

    /**
     * Activates Dragon's Hunger for {@code player}.
     *
     * <p>Does nothing if the ability is already active for this player or is
     * on cooldown.
     *
     * @param player the bearer who triggered the ability
     */
    public void activateDragonHunger(ServerPlayer player) {
        if (state == AbilityState.ACTIVE && player.getUUID().equals(activePlayerUUID)) {
            return; // already active for this player
        }
        if (state == AbilityState.COOLDOWN) {
            DragonsLegacyMod.LOGGER.debug(
                "[Dragon's Legacy] Dragon's Hunger on cooldown for {} ({} ticks remaining).",
                player.getGameProfile().getName(), timers.getCooldownRemaining()
            );
            return;
        }

        // Apply effects
        timers.resetForNewActivation();
        DragonHungerAbility.apply(player);

        state            = AbilityState.ACTIVE;
        activePlayerUUID = player.getUUID();

        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Dragon's Hunger activated for {} ({} ticks).",
            player.getGameProfile().getName(), AbilityTimers.DEFAULT_DURATION
        );

        publishEvent(new AbilityActivatedEvent(player.getUUID(), AbilityTimers.DEFAULT_DURATION));
    }

    /**
     * Manually deactivates Dragon's Hunger for {@code player} with a reason string.
     *
     * <p>If the ability is not currently active for {@code player} this is a no-op.
     *
     * @param player the player whose ability should be stopped
     * @param reason machine-readable reason (e.g. {@code "head_removed"})
     */
    public void deactivateDragonHunger(ServerPlayer player, String reason) {
        if (state != AbilityState.ACTIVE) return;
        if (!player.getUUID().equals(activePlayerUUID)) return;

        DragonHungerAbility.remove(player);
        timers.startCooldown();
        state = AbilityState.COOLDOWN;

        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Dragon's Hunger deactivated for {} (reason: {}).",
            player.getGameProfile().getName(), reason
        );

        publishEvent(new AbilityDeactivatedEvent(player.getUUID(), reason));
        activePlayerUUID = null;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the ability is currently active for {@code player}.
     */
    public boolean isActive(ServerPlayer player) {
        return state == AbilityState.ACTIVE && player.getUUID().equals(activePlayerUUID);
    }

    /**
     * Returns {@code true} if the ability is on cooldown (regardless of which player
     * previously activated it).
     */
    public boolean isOnCooldown(ServerPlayer player) {
        return state == AbilityState.COOLDOWN;
    }

    /** Returns the current {@link AbilityState}. */
    public AbilityState getState() { return state; }

    /** Returns the {@link AbilityTimers} so callers can inspect remaining ticks. */
    public AbilityTimers getTimers() { return timers; }

    // -------------------------------------------------------------------------
    // Cooldown helpers (called by EggEventHandler)
    // -------------------------------------------------------------------------

    /**
     * Resets the cooldown so a new bearer can activate the ability immediately.
     * Has no effect if the ability is currently active.
     */
    public void resetCooldownIfNeeded() {
        if (state == AbilityState.COOLDOWN) {
            timers.clearCooldown();
            state = AbilityState.INACTIVE;
            DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Dragon's Hunger cooldown cleared for new bearer.");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void expireAbility(MinecraftServer server) {
        UUID expiredFor = activePlayerUUID;
        state            = AbilityState.COOLDOWN;
        activePlayerUUID = null;
        timers.startCooldown();

        // Best-effort: attempt to remove effects from the still-online player
        if (expiredFor != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(expiredFor);
            if (player != null) {
                DragonHungerAbility.remove(player);
            }
        }

        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Dragon's Hunger expired.");
        if (expiredFor != null) {
            publishEvent(new AbilityExpiredEvent(expiredFor));
        }
    }

    /**
     * Publishes an ability event on the shared {@link DragonEggEventBus}.
     * {@link AbilityEvent} extends {@link dev.dragonslegacy.egg.event.DragonEggEvent} so no
     * casting is required.
     */
    private <T extends dev.dragonslegacy.ability.event.AbilityEvent> void publishEvent(T event) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;
        legacy.getEventBus().publish(event);
    }
}
