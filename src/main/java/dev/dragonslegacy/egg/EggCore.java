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
 * A stack is the <em>canonical</em> egg if and only if:
 * <ol>
 *   <li>{@code stack.getItem() == Items.DRAGON_EGG}</li>
 *   <li>{@code dragonslegacy:egg == true}</li>
 *   <li>{@code dragonslegacy:egg_id == canonicalEggId} (stored in world-persistent data)</li>
 * </ol>
 * Use {@link #isDragonEgg(ItemStack)} as the fast filter and
 * {@link #isCanonicalEgg(ItemStack, UUID)} for the full canonical check.
 *
 * <h3>State machine</h3>
 * <ul>
 *   <li>{@link EggState#PLAYER}        – egg is in an online player's inventory</li>
 *   <li>{@link EggState#OFFLINE_PLAYER} – egg is in an offline player's inventory</li>
 *   <li>{@link EggState#WORLD}          – egg is a dropped item entity</li>
 *   <li>{@link EggState#BLOCK}          – egg is placed as a block</li>
 *   <li>{@link EggState#UNKNOWN}        – egg cannot be located</li>
 * </ul>
 *
 * <h3>Fallback spawn</h3>
 * If the egg remains {@link EggState#UNKNOWN} for longer than
 * {@value #FALLBACK_GRACE_TICKS} ticks a new tagged egg is spawned at the
 * world spawn point.
 *
 * <h3>UUID scrambling</h3>
 * Every {@value #SCRAMBLE_INTERVAL_TICKS} ticks, when the canonical egg is
 * positively located, its {@code dragonslegacy:egg_id} component is replaced
 * with a fresh UUID and the stored canonical ID is updated atomically.
 * This ensures any copies of the egg become counterfeits on the next scrub pass.
 */
public class EggCore {

    /**
     * Grace period before the fallback spawn triggers: 5 minutes (6 000 ticks).
     * This prevents spurious respawns during brief scan gaps.
     */
    public static final int FALLBACK_GRACE_TICKS = 6_000;

    /**
     * Interval between UUID scrambles: 60 seconds (1 200 ticks).
     * Configurable if needed; changing this only affects how quickly copies expire.
     */
    public static final int SCRAMBLE_INTERVAL_TICKS = 1_200;

    private final EggPersistentState persistentState;
    private final EggTracker         eggTracker;
    private final EggSpawnFallback   spawnFallback;

    /** Server tick at which the egg first became UNKNOWN, or −1 when not UNKNOWN. */
    private int unknownSinceTick = -1;
    /** Last EggState used for transition-logging. */
    private EggState lastLoggedState = null;
    /** Server tick at which the last UUID scramble occurred. */
    private int lastScrambleTick = -1;
    /** When {@code true}, the anti-dupe engine will run on the very next server tick. */
    private volatile boolean pendingImmediateScrub = false;

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
     * Fast filter: returns {@code true} if the stack is a {@link Items#DRAGON_EGG}
     * with {@code dragonslegacy:egg = true}.
     *
     * <p><strong>This does NOT verify the UUID.</strong>  Use
     * {@link #isCanonicalEgg(ItemStack, UUID)} for the full canonical check.
     */
    public static boolean isDragonEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return false;
        return Boolean.TRUE.equals(stack.get(EggComponents.EGG));
    }

    /**
     * Full canonical check: returns {@code true} iff the stack is a
     * {@link Items#DRAGON_EGG} with both {@code dragonslegacy:egg = true}
     * AND {@code dragonslegacy:egg_id == canonicalId}.
     *
     * @param stack       the stack to inspect
     * @param canonicalId the world-persistent canonical UUID; if {@code null}
     *                    this method always returns {@code false}
     */
    public static boolean isCanonicalEgg(ItemStack stack, @Nullable UUID canonicalId) {
        if (canonicalId == null) return false;
        if (!isDragonEgg(stack)) return false;
        return canonicalId.equals(stack.get(EggComponents.EGG_ID));
    }

    /**
     * Tags the given Dragon Egg {@link ItemStack} with
     * {@code dragonslegacy:egg = true} and {@code dragonslegacy:egg_id = canonicalId}.
     *
     * <p>If {@code canonicalId} is {@code null} a new UUID is generated and
     * immediately persisted via {@link DragonsLegacy#getInstance()}.
     *
     * @param stack must be a {@link Items#DRAGON_EGG} stack
     */
    public static void tagEgg(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return;
        stack.set(EggComponents.EGG, true);

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        UUID canonicalId = legacy != null ? legacy.getPersistentState().getCanonicalEggId() : null;
        if (canonicalId == null) {
            canonicalId = UUID.randomUUID();
            if (legacy != null) {
                legacy.getPersistentState().setCanonicalEggId(canonicalId);
                DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Canonical Dragon Egg ID established: {}",
                    canonicalId);
            }
        }
        stack.set(EggComponents.EGG_ID, canonicalId);
    }

    /**
     * Tags the given Dragon Egg {@link ItemStack} with
     * {@code dragonslegacy:egg = true} and the provided {@code canonicalId}.
     * Use this when the canonical ID is already known (e.g. fallback respawn).
     *
     * @param stack       must be a {@link Items#DRAGON_EGG} stack
     * @param canonicalId the UUID to stamp on the item; must not be {@code null}
     */
    public static void tagEgg(ItemStack stack, UUID canonicalId) {
        if (stack.isEmpty() || !stack.is(Items.DRAGON_EGG)) return;
        stack.set(EggComponents.EGG, true);
        stack.set(EggComponents.EGG_ID, canonicalId);
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

    /**
     * Resolves the display name for a bearer UUID, including offline players.
     * Looks up the online player first; if offline, queries the server's
     * name-to-id cache.  Falls back to the UUID string if the name cannot be
     * resolved.
     *
     * @param bearerUuid the UUID to resolve
     * @param server     the current {@link MinecraftServer}
     * @return a human-readable player name, or the UUID string as a fallback
     */
    public static String resolveBearerName(UUID bearerUuid, MinecraftServer server) {
        ServerPlayer online = server.getPlayerList().getPlayer(bearerUuid);
        if (online != null) return online.getGameProfile().name();
        net.minecraft.server.players.NameAndId nameAndId =
            server.services().nameToIdCache().get(bearerUuid).orElse(null);
        return nameAndId != null ? nameAndId.name() : bearerUuid.toString();
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
     * Requests that the anti-dupe engine's {@code scanAndResolveDuplicates} runs on
     * the next server tick, regardless of the normal interval.
     * Safe to call from any thread.
     */
    public void requestImmediateScrub() {
        pendingImmediateScrub = true;
    }

    /**
     * Returns {@code true} and clears the pending flag if an immediate scrub was
     * requested, {@code false} otherwise.  Intended for use by the tick handler only.
     */
    public boolean consumeImmediateScrub() {
        if (!pendingImmediateScrub) return false;
        pendingImmediateScrub = false;
        return true;
    }

    /**
     * Heartbeat: re-scans for the egg in all possible locations, updates state,
     * and triggers the fallback spawn when the egg is missing long enough.
     * Also schedules periodic UUID scrambling.
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

    /**
     * Scrambles the canonical egg UUID when:
     * <ul>
     *   <li>the canonical egg is currently located (not {@link EggState#UNKNOWN})</li>
     *   <li>enough time has elapsed since the last scramble</li>
     * </ul>
     * The method finds the canonical egg's {@link ItemStack}, atomically updates
     * both the item component and the persistent canonical ID, then logs the
     * event (to console/logs only – never exposed to players).
     */
    public void maybeScramble(MinecraftServer server) {
        int currentTick = server.getTickCount();
        if (lastScrambleTick >= 0 && currentTick - lastScrambleTick < SCRAMBLE_INTERVAL_TICKS) return;

        EggState state = eggTracker.getCurrentState();
        if (state == EggState.UNKNOWN) return;

        UUID currentId = persistentState.getCanonicalEggId();
        if (currentId == null) return;

        // Locate the canonical item stack so we can update its component
        ItemStack canonicalStack = findCanonicalStack(server, currentId);
        if (canonicalStack == null || canonicalStack.isEmpty()) return;

        UUID newId = UUID.randomUUID();
        // Atomic: update item component and persistent ID together
        canonicalStack.set(EggComponents.EGG_ID, newId);
        persistentState.setCanonicalEggId(newId);
        lastScrambleTick = currentTick;

        logScramble(currentId, newId);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Locates and returns the {@link ItemStack} that holds the canonical egg
     * in player inventories or as an item entity.
     * Returns {@code null} when the egg is placed as a block (no item stack)
     * or cannot be found.
     */
    private @Nullable ItemStack findCanonicalStack(MinecraftServer server, UUID canonicalId) {
        // Check online player inventories
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (isCanonicalEgg(stack, canonicalId)) return stack;
            }
            for (net.minecraft.world.entity.EquipmentSlot slot
                    : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (isCanonicalEgg(stack, canonicalId)) return stack;
            }
        }
        // Check dropped item entities
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                border.getMinX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), dev.dragonslegacy.utils.Utils.WORLD_Y_MAX, border.getMaxZ());
            for (net.minecraft.world.entity.item.ItemEntity item
                    : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
                if (isCanonicalEgg(item.getItem(), canonicalId)) return item.getItem();
            }
        }
        return null;
    }

    private void triggerFallback(MinecraftServer server, int currentTick) {
        if (!persistentState.isEggInitialized()) return;
        // Never trigger fallback when the egg is safely held by an offline bearer
        if (eggTracker.getCurrentBearer() != null &&
                server.getPlayerList().getPlayer(eggTracker.getCurrentBearer()) == null) {
            logEggEvent("Skipping fallback spawn: bearer is offline.");
            return;
        }
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

    private void logScramble(UUID oldId, UUID newId) {
        var logging = DragonsLegacyMod.configManager.getLogging();
        if (logging != null && logging.logging.enabled) {
            DragonsLegacyMod.LOGGER.debug(
                "[Dragon's Legacy] Canonical egg UUID scrambled: {} → {}",
                oldId, newId
            );
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
            case OFFLINE_PLAYER -> {
                @Nullable UUID offlineUuid = eggTracker.getCurrentBearer();
                DragonsLegacyMod.LOGGER.debug(
                    "[Dragon's Legacy] Egg state: OFFLINE_PLAYER (bearer={}) – egg is safely held by offline player",
                    offlineUuid != null ? offlineUuid : "unknown");
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
