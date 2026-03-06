package dev.dragonslegacy.egg;

import de.arvitus.dragonegggame.DragonEggGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Removes the bearer designation when the bearer has been offline for too long.
 *
 * <p>The threshold is configurable via {@link #setOfflineThresholdDays(double)};
 * the default is 3 real-world days (259 200 000 ms wall-clock time).
 */
public class EggOfflineResetManager {

    /** Default time before a missing bearer is cleared: 3 in-game days (ms). */
    private static final long DEFAULT_THRESHOLD_MS = 3L * 24L * 60L * 60L * 1000L;

    private long thresholdMs = DEFAULT_THRESHOLD_MS;

    /** Wall-clock time (ms) at which the current bearer last came online. */
    private long bearerLastOnlineMs = -1L;

    private final EggPersistentState persistentState;

    EggOfflineResetManager(EggPersistentState persistentState) {
        this.persistentState = persistentState;
    }

    // -------------------------------------------------------------------------
    // Fabric event delegates
    // -------------------------------------------------------------------------

    /** Called when a player joins the server. */
    public void onPlayerJoin(ServerPlayer player) {
        @Nullable UUID bearer = persistentState.getBearerUUID();
        if (bearer != null && bearer.equals(player.getUUID())) {
            bearerLastOnlineMs = System.currentTimeMillis();
            persistentState.setBearerLastSeenTick(player.serverLevel().getGameTime());
            DragonEggGame.LOGGER.info(
                "[Dragon's Legacy] Bearer {} came online – resetting offline timer.", player.getName().getString()
            );
        }
    }

    /** Called when a player leaves the server. */
    public void onPlayerLeave(ServerPlayer player) {
        @Nullable UUID bearer = persistentState.getBearerUUID();
        if (bearer != null && bearer.equals(player.getUUID())) {
            bearerLastOnlineMs = System.currentTimeMillis();
            persistentState.setBearerLastSeenTick(player.serverLevel().getGameTime());
        }
    }

    /**
     * Called on each server tick (or at a reduced frequency) to check whether the
     * bearer has been offline long enough to trigger a reset.
     */
    public void tick(MinecraftServer server) {
        @Nullable UUID bearer = persistentState.getBearerUUID();
        if (bearer == null) return;

        // Bearer is online – nothing to do
        if (server.getPlayerList().getPlayer(bearer) != null) return;

        // Bearer is offline – check wall-clock time
        if (bearerLastOnlineMs < 0) {
            // Initialise from persistent state (rough estimate)
            bearerLastOnlineMs = System.currentTimeMillis()
                - (server.overworld().getGameTime() - persistentState.getBearerLastSeenTick()) * 50L;
        }

        double daysOffline = getDaysOffline();
        if (daysOffline >= getOfflineThresholdDays()) {
            DragonEggGame.LOGGER.info(
                "[Dragon's Legacy] Bearer {} has been offline for {} days – clearing bearer.",
                bearer, String.format("%.1f", daysOffline)
            );
            clearBearer();
        }
    }

    /** Returns how many real days the bearer has been offline. */
    public double getDaysOffline() {
        if (bearerLastOnlineMs < 0) return 0.0;
        long elapsed = System.currentTimeMillis() - bearerLastOnlineMs;
        return elapsed / (double) (24L * 60L * 60L * 1000L);
    }

    /** Returns the configured offline threshold in days. */
    public double getOfflineThresholdDays() {
        return thresholdMs / (double) (24L * 60L * 60L * 1000L);
    }

    /** Sets a new offline threshold (in days). */
    public void setOfflineThresholdDays(double days) {
        this.thresholdMs = (long) (days * 24L * 60L * 60L * 1000L);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void clearBearer() {
        persistentState.setBearerUUID(null);
        bearerLastOnlineMs = -1L;
        DragonEggGame.LOGGER.info("[Dragon's Legacy] Bearer cleared due to prolonged absence.");
    }
}
