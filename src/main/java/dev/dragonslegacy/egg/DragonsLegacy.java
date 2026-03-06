package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.announce.AnnouncementManager;
import dev.dragonslegacy.config.ConfigManager;
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
    private final ConfigManager          configManager;
    private final DragonEggEventBus      eventBus;
    private final EggPersistentState     persistentState;
    private final EggIdentityManager     identityManager;
    private final EggTracker             eggTracker;
    private final EggSpawnFallback       eggSpawnFallback;
    private final EggAntiDupeEngine      eggAntiDupeEngine;
    private final EggProtectionManager   eggProtectionManager;
    private final EggOfflineResetManager eggOfflineResetManager;
    private final AbilityEngine          abilityEngine;
    private final AnnouncementManager    announcementManager;

    // ------------------------------------------------------------------
    // Construction (private – use getInstance() after init())
    // ------------------------------------------------------------------

    private DragonsLegacy(EggPersistentState persistentState, ConfigManager configManager) {
        this.configManager        = configManager;
        this.persistentState      = persistentState;
        this.eventBus             = new DragonEggEventBus();
        this.identityManager      = new EggIdentityManager();
        this.eggTracker           = new EggTracker(persistentState, eventBus);
        this.eggSpawnFallback     = new EggSpawnFallback(eventBus);
        this.eggAntiDupeEngine    = new EggAntiDupeEngine(identityManager);
        this.eggProtectionManager = new EggProtectionManager(eggSpawnFallback);
        this.eggOfflineResetManager = new EggOfflineResetManager(persistentState);
        this.abilityEngine        = new AbilityEngine();
        this.announcementManager  = new AnnouncementManager();
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
        // 1. Load config first so values are available to all subsystems
        ConfigManager configManager = new ConfigManager();
        configManager.init();

        // 2. Load or create persistent state from overworld storage
        EggPersistentState state = server.overworld()
            .getDataStorage()
            .computeIfAbsent(EggPersistentState.FACTORY, EggPersistentState.SAVE_ID);

        DragonsLegacy legacy = new DragonsLegacy(state, configManager);

        // 3. Sync identity manager with loaded state
        if (state.getCanonicalEggId() != null) {
            legacy.identityManager.setCanonicalEggId(state.getCanonicalEggId());
        }

        INSTANCE = legacy;

        // 4. Apply config values to subsystems before they are started
        legacy.applyConfig();

        // 5. Start subsystems
        legacy.abilityEngine.init(server);
        legacy.announcementManager.init(server, legacy.eventBus);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Subsystem initialised.");
    }

    /**
     * Reloads {@code config.yml} from disk and re-applies all values to the
     * running subsystems.  Called from {@code /dragonslegacy reload}.
     */
    public void reload() {
        configManager.reload();
        applyConfig();
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Configuration reloaded.");
    }

    /**
     * Pushes the current {@link ConfigManager} values into every subsystem that
     * honours them.  Safe to call multiple times (e.g. on reload).
     */
    private void applyConfig() {
        // Ability timers
        abilityEngine.getTimers().setConfiguredDuration(configManager.getAbilityDurationTicks());
        abilityEngine.getTimers().setConfiguredCooldown(configManager.getAbilityCooldownTicks());

        // Offline-reset threshold
        eggOfflineResetManager.setOfflineThresholdDays(configManager.getOfflineResetDays());

        // Spawn-fallback toggle
        eggSpawnFallback.setEnabled(configManager.getSpawnFallbackEnabled());

        // Announcement templates
        announcementManager.setTemplates(configManager.getAnnouncementTemplates());
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
    public AnnouncementManager    getAnnouncementManager()    { return announcementManager; }
    public ConfigManager          getConfigManager()          { return configManager; }
}
