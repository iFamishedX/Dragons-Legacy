package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Central authority for the canonical Dragon Egg's state, bearer, and identity.
 *
 * <h3>Egg identity</h3>
 * The canonical egg is identified solely by the {@code dragonslegacy:egg} boolean
 * data component ({@link EggComponents#EGG}).  Use {@link #isDragonEgg(ItemStack)}
 * as the <em>single</em> authoritative detection check throughout the codebase.
 *
 * <h3>State machine</h3>
 * <ul>
 *   <li>{@link EggState#PLAYER}  – egg is in a player's inventory</li>
 *   <li>{@link EggState#WORLD}   – egg is a dropped item entity</li>
 *   <li>{@link EggState#BLOCK}   – egg is placed as a block</li>
 *   <li>{@link EggState#UNKNOWN} – egg cannot be located</li>
 * </ul>
 *
 * <h3>Fallback spawn</h3>
 * If the egg remains {@link EggState#UNKNOWN} for longer than
 * {@value #FALLBACK_GRACE_TICKS} ticks a new tagged egg is spawned at the
 * world spawn point.
 */
public class EggCore {

    /**
     * Grace period before the fallback spawn triggers: 5 minutes (6 000 ticks).
     * This prevents spurious respawns during brief scan gaps.
     */
    public static final int FALLBACK_GRACE_TICKS = 6_000;

    private final EggPersistentState persistentState;
    private final EggTracker         eggTracker;
    private final EggSpawnFallback   spawnFallback;

    /** Server tick at which the egg first became UNKNOWN, or −1 when not UNKNOWN. */
    private int unknownSinceTick = -1;
    /** Last EggState used for transition-logging. */
    private EggState lastLoggedState = null;

    EggCore(EggPersistentState persistentState,
            EggTracker eggTracker,
            EggSpawnFallback spawnFallback) {
        this.persistentState = persistentState;
        this.eggTracker      = eggTracker;
        this.spawnFallback   = spawnFallback;
    }

    // =========================================================================
    // Static identity API  (no instance required)
    // =========================================================================

    /**
     * The <strong>single authoritative</strong> check for whether a given
     * {@link ItemStack} is the canonical Dragon Egg.
     *
     * @return {@code true} iff:
     *         <ul>
     *           <li>{@code stack.getItem() == Items.DRAGON_EGG}, AND</li>
     *           <li>the {@code dragonslegacy:egg} component is {@code true}</li>
     *         </ul>
     */
    public static boolean isDragonEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return false;
        return Boolean.TRUE.equals(stack.get(EggComponents.EGG));
    }

    /**
     * Tags the given Dragon Egg {@link ItemStack} with
     * {@code dragonslegacy:egg = true}, marking it as the canonical egg.
     *
     * @param stack must be a {@link Items#DRAGON_EGG} stack
     */
    public static void tagEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return;
        stack.set(EggComponents.EGG, true);
    }

    // =========================================================================
    // Instance API
    // =========================================================================

    /** Returns the UUID of the current bearer, or an empty optional. */
    public Optional<UUID> getBearerUuid() {
        return Optional.ofNullable(eggTracker.getCurrentBearer());
    }

    /**
     * Returns the online {@link ServerPlayer} who is the current bearer, or
     * an empty optional if there is no bearer or the bearer is offline.
     */
    public Optional<ServerPlayer> getBearerPlayer(MinecraftServer server) {
        return Optional.ofNullable(eggTracker.getBearerPlayer(server));
    }

    /** Returns the current {@link EggState}. */
    public EggState getEggState() {
        return eggTracker.getCurrentState();
    }

    /**
     * Returns the current egg location, or an empty optional when
     * state is {@link EggState#UNKNOWN}.
     */
    public Optional<EggLocation> getEggLocation() {
        EggState state = eggTracker.getCurrentState();
        if (state == EggState.UNKNOWN) return Optional.empty();
        BlockPos pos             = eggTracker.getPlacedLocation();
        ResourceKey<Level> dim   = eggTracker.getEggDimension();
        return Optional.of(new EggLocation(state, pos, dim));
    }

    /**
     * Force-sets the bearer to the given player, updating tracker and
     * persistent state.
     */
    public void setBearer(ServerPlayer player) {
        eggTracker.updateEggHeld(player);
    }

    /** Clears the current bearer designation without changing egg position. */
    public void clearBearer() {
        eggTracker.clearBearer();
    }

    // =========================================================================
    // Pickup / placement hooks
    // =========================================================================

    /**
     * Called when a player picks up the Dragon Egg.
     * Tags the stack with the canonical component and transitions state to
     * {@link EggState#PLAYER}.
     */
    public void onEggPickedUp(ServerPlayer player, ItemStack stack) {
        tagEgg(stack);
        eggTracker.updateEggHeld(player);
        logEggEvent("Egg picked up by player {}.", player.getGameProfile().name());
    }

    /**
     * Called when the canonical egg is placed as a block.
     * Transitions state to {@link EggState#BLOCK}.
     */
    public void onEggPlaced(BlockPos pos, Level level) {
        eggTracker.updateEggPlaced(pos, level.dimension());
        logEggEvent("Egg placed as block at {}.", pos.toShortString());
    }

    /**
     * Called when the canonical egg block is broken.
     * Clears the tracked block position; state will become {@link EggState#WORLD}
     * when the dropped item entity is detected.
     */
    public void onEggBroken(BlockPos pos, Level level) {
        eggTracker.clearPlacedLocation();
        logEggEvent("Egg block broken at {}.", pos.toShortString());
    }

    // =========================================================================
    // Heartbeat tick
    // =========================================================================

    /**
     * Heartbeat: re-scans for the egg in all possible locations, updates state,
     * and triggers the fallback spawn when the egg is missing long enough.
     *
     * <p>Should be called every {@code TICK_INTERVAL} ticks (e.g. every 100 ticks).
     */
    public void tick(MinecraftServer server) {
        EggState beforeState = eggTracker.getCurrentState();

        // 1. Full re-scan (player inventories → world entities → placed block)
        eggTracker.scanAndLocateEgg(server);

        EggState afterState = eggTracker.getCurrentState();

        // 2. Debug: log state transitions
        logStateTransition(beforeState, afterState, server);

        // 3. Fallback spawn when UNKNOWN exceeds grace period
        if (afterState == EggState.UNKNOWN) {
            int currentTick = server.getTickCount();
            if (unknownSinceTick < 0) {
                unknownSinceTick = currentTick;
            } else if (currentTick - unknownSinceTick >= FALLBACK_GRACE_TICKS) {
                triggerFallback(server, currentTick);
            }
        } else {
            unknownSinceTick = -1;
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private void triggerFallback(MinecraftServer server, int currentTick) {
        if (!persistentState.isEggInitialized()) return;
        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Egg has been UNKNOWN for {} ticks – triggering fallback spawn.",
            currentTick - unknownSinceTick
        );
        spawnFallback.ensureEggExists(server);
        unknownSinceTick = -1;
    }

    private void logEggEvent(String msg, Object... args) {
        var logging = DragonsLegacyMod.configManager.getLogging();
        if (logging != null && logging.logging.enabled && logging.logging.eggEvents.console) {
            DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] " + msg, args);
        }
    }

    private void logStateTransition(EggState before, EggState after, MinecraftServer server) {
        if (before == after && after == lastLoggedState) return; // suppress duplicate logs
        var logging = DragonsLegacyMod.configManager.getLogging();
        if (logging == null || !logging.logging.enabled || !logging.logging.stateChanges.console) return;

        if (before != after) {
            DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Egg state: {} → {}", before, after);
        }

        switch (after) {
            case PLAYER -> {
                @Nullable ServerPlayer bearer = eggTracker.getBearerPlayer(server);
                DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Egg state: PLAYER (bearer={})",
                    bearer != null ? bearer.getGameProfile().name() : "unknown");
            }
            case WORLD -> {
                @Nullable BlockPos worldPos = eggTracker.getPlacedLocation();
                DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Egg state: WORLD (dropped item at {})",
                    worldPos != null ? worldPos.toShortString() : "unknown pos");
            }
            case BLOCK -> {
                @Nullable BlockPos pos = eggTracker.getPlacedLocation();
                DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Egg state: BLOCK (pos={})",
                    pos != null ? pos.toShortString() : "unknown");
            }
            case UNKNOWN -> DragonsLegacyMod.LOGGER.debug("[Dragon's Legacy] Egg state: UNKNOWN");
        }
        lastLoggedState = after;
    }
}
