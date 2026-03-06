package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Singleton coordinator for all Dragon's Legacy subsystems.
 *
 * <p>Call {@link #init(MinecraftServer)} once when the server starts (from
 * {@link DragonsLegacyMod#onInitialize()}) and {@link #shutdown()} when it stops.
 */
public class DragonsLegacy {

    private static @Nullable DragonsLegacy INSTANCE;

    // ------------------------------------------------------------------
    // Sub-managers
    // ------------------------------------------------------------------
    private final DragonEggEventBus      eventBus;
    private final EggPersistentState     persistentState;
    private final EggIdentityManager     identityManager;
    private final EggTracker             eggTracker;
    private final EggSpawnFallback       eggSpawnFallback;
    private final EggAntiDupeEngine      eggAntiDupeEngine;
    private final EggProtectionManager   eggProtectionManager;
    private final EggOfflineResetManager eggOfflineResetManager;
    private final AbilityEngine          abilityEngine;

    // ------------------------------------------------------------------
    // Construction (private – use getInstance() after init())
    // ------------------------------------------------------------------

    private DragonsLegacy(EggPersistentState persistentState) {
        this.persistentState      = persistentState;
        this.eventBus             = new DragonEggEventBus();
        this.identityManager      = new EggIdentityManager();
        this.eggTracker           = new EggTracker(persistentState, eventBus);
        this.eggSpawnFallback     = new EggSpawnFallback(eventBus);
        this.eggAntiDupeEngine    = new EggAntiDupeEngine(identityManager);
        this.eggProtectionManager = new EggProtectionManager(eggSpawnFallback);
        this.eggOfflineResetManager = new EggOfflineResetManager(persistentState);
        this.abilityEngine        = new AbilityEngine();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Initialises Dragon's Legacy, loading the {@link EggPersistentState} from
     * the overworld's {@link net.minecraft.world.level.storage.DimensionDataStorage}
     * and wiring all sub-managers.
     *
     * @param server the running {@link MinecraftServer}
     */
    public static synchronized void init(MinecraftServer server) {
        // Load or create persistent state from overworld storage
        EggPersistentState state = server.overworld()
            .getDataStorage()
            .computeIfAbsent(EggPersistentState.FACTORY, EggPersistentState.SAVE_ID);

        DragonsLegacy legacy = new DragonsLegacy(state);

        // Sync identity manager with loaded state
        if (state.getCanonicalEggId() != null) {
            legacy.identityManager.setCanonicalEggId(state.getCanonicalEggId());
        }

        INSTANCE = legacy;
        legacy.abilityEngine.init(server);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Subsystem initialised.");
    }

    /**
     * Cleans up resources and clears the singleton instance.
     * Called when the server is stopping.
     */
    public void shutdown() {
        eventBus.clearListeners();
        INSTANCE = null;
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Subsystem shut down.");
    }

    // ------------------------------------------------------------------
    // Singleton access
    // ------------------------------------------------------------------

    /**
     * Returns the current singleton, or {@code null} if
     * {@link #init(MinecraftServer)} has not yet been called.
     */
    public static @Nullable DragonsLegacy getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public DragonEggEventBus      getEventBus()              { return eventBus; }
    public EggPersistentState     getPersistentState()        { return persistentState; }
    public EggIdentityManager     getEggIdentityManager()     { return identityManager; }
    public EggTracker             getEggTracker()             { return eggTracker; }
    public EggSpawnFallback       getEggSpawnFallback()       { return eggSpawnFallback; }
    public EggAntiDupeEngine      getEggAntiDupeEngine()      { return eggAntiDupeEngine; }
    public EggProtectionManager   getEggProtectionManager()   { return eggProtectionManager; }
    public EggOfflineResetManager getEggOfflineResetManager() { return eggOfflineResetManager; }
    public AbilityEngine          getAbilityEngine()          { return abilityEngine; }
}
