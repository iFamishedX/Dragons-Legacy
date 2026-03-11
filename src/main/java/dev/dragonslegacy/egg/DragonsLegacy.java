package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.ability.PassiveEffectsEngine;
import dev.dragonslegacy.announce.AnnouncementManager;
import dev.dragonslegacy.config.AbilityConfig;
import dev.dragonslegacy.config.AnnouncementsConfig;
import dev.dragonslegacy.config.AttributeEntry;
import dev.dragonslegacy.config.ConfigAttributeParser;
import dev.dragonslegacy.config.ConfigEffectParser;
import dev.dragonslegacy.config.ConfigManager;
import dev.dragonslegacy.config.EffectEntry;
import dev.dragonslegacy.config.PassiveEffectsConfig;
import dev.dragonslegacy.config.SpawnConfig;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
    private final EggCore                eggCore;
    private final EggSpawnFallback       eggSpawnFallback;
    private final EggAntiDupeEngine      eggAntiDupeEngine;
    private final EggProtectionManager   eggProtectionManager;
    private final EggOfflineResetManager eggOfflineResetManager;
    private final AbilityEngine          abilityEngine;
    private final PassiveEffectsEngine   passiveEffectsEngine;
    private final AnnouncementManager    announcementManager;

    // ------------------------------------------------------------------
    // Construction (private – use getInstance() after init())
    // ------------------------------------------------------------------

    private DragonsLegacy(EggPersistentState persistentState) {
        this.persistentState      = persistentState;
        this.eventBus             = new DragonEggEventBus();
        this.identityManager      = new EggIdentityManager();
        this.eggTracker           = new EggTracker(persistentState, eventBus);
        this.eggSpawnFallback     = new EggSpawnFallback(eventBus);
        this.eggCore              = new EggCore(persistentState, eggTracker, eggSpawnFallback);
        this.eggAntiDupeEngine    = new EggAntiDupeEngine();
        this.eggProtectionManager = new EggProtectionManager(eggSpawnFallback);
        this.eggOfflineResetManager = new EggOfflineResetManager(persistentState);
        this.abilityEngine        = new AbilityEngine();
        this.passiveEffectsEngine = new PassiveEffectsEngine();
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
        // ConfigManager is already initialised in DragonsLegacyMod.onInitialize()

        // Load or create persistent state from overworld storage
        EggPersistentState state = server.overworld()
            .getDataStorage()
            .computeIfAbsent(EggPersistentState.TYPE);

        DragonsLegacy legacy = new DragonsLegacy(state);

        INSTANCE = legacy;

        // Apply config values to subsystems before they are started
        legacy.applyConfig();

        // Start subsystems
        legacy.abilityEngine.init(server);
        legacy.announcementManager.init(server, legacy.eventBus);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Subsystem initialised.");
    }

    /**
     * Reloads all config files from disk and re-applies all values to the
     * running subsystems.  Called from {@code /dragonslegacy reload}.
     *
     * <p>If a player is currently the bearer, passive effects are removed,
     * configs are reloaded, and passive effects are re-applied so the
     * bearer always reflects the latest config.
     *
     * @param server the running {@link MinecraftServer} (used to locate the current bearer)
     */
    public void reload(MinecraftServer server) {
        // Remove passive effects from the current bearer before reloading config
        ServerPlayer currentBearer = eggTracker.getBearerPlayer(server);
        if (currentBearer != null) {
            passiveEffectsEngine.removeFromPlayer(currentBearer);
        }

        DragonsLegacyMod.configManager.reload();
        applyConfig();

        // Re-apply passive effects with the new config
        if (currentBearer != null) {
            passiveEffectsEngine.applyToBearer(currentBearer);
        }

        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Configuration reloaded.");
    }

    /**
     * Pushes the current {@link ConfigManager} values into every subsystem that
     * honours them.  Safe to call multiple times (e.g. on reload).
     */
    private void applyConfig() {
        AbilityConfig ability = DragonsLegacyMod.configManager.getAbility();
        abilityEngine.getTimers().setConfiguredDuration(ability.durationTicks);
        abilityEngine.getTimers().setConfiguredCooldown(ability.cooldownTicks);

        SpawnConfig spawn = DragonsLegacyMod.configManager.getSpawn();
        eggSpawnFallback.setEnabled(spawn.fallbackEnabled);

        AnnouncementsConfig ann = DragonsLegacyMod.configManager.getAnnouncements();
        announcementManager.setTemplates(ann.templates);
        announcementManager.setUseMiniMessage(ann.useMiniMessage);

        validateConfigs();
    }

    /**
     * Eagerly validates all effect and attribute entries in the loaded configs
     * by resolving them against the Minecraft registries.
     */
    private void validateConfigs() {
        AbilityConfig ability = DragonsLegacyMod.configManager.getAbility();
        PassiveEffectsConfig passive = DragonsLegacyMod.configManager.getPassiveEffects();

        validateEffects(ability.effects);
        validateAttributes(ability.attributes);
        validateEffects(passive.effects);
        validateAttributes(passive.attributes);
    }

    private static void validateEffects(java.util.List<EffectEntry> entries) {
        if (entries == null) return;
        for (EffectEntry e : entries) {
            ConfigEffectParser.parseEffect(e);
        }
    }

    private static void validateAttributes(java.util.List<AttributeEntry> entries) {
        if (entries == null) return;
        for (AttributeEntry a : entries) {
            ConfigAttributeParser.parseAttribute(a);
        }
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
    /** @deprecated Use {@link #getEggCore()} instead. */
    @Deprecated
    public EggIdentityManager     getEggIdentityManager()     { return identityManager; }
    public EggTracker             getEggTracker()             { return eggTracker; }
    public EggCore                getEggCore()                { return eggCore; }
    public EggSpawnFallback       getEggSpawnFallback()       { return eggSpawnFallback; }
    public EggAntiDupeEngine      getEggAntiDupeEngine()      { return eggAntiDupeEngine; }
    public EggProtectionManager   getEggProtectionManager()   { return eggProtectionManager; }
    public EggOfflineResetManager getEggOfflineResetManager() { return eggOfflineResetManager; }
    public AbilityEngine          getAbilityEngine()          { return abilityEngine; }
    public PassiveEffectsEngine   getPassiveEffectsEngine()   { return passiveEffectsEngine; }
    public AnnouncementManager    getAnnouncementManager()    { return announcementManager; }
    public ConfigManager          getConfigManager()          { return DragonsLegacyMod.configManager; }
}
