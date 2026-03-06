package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the Dragon's Legacy YAML configuration file at
 * {@code config/dragonslegacy/config.yml}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Instantiate once inside {@link dev.dragonslegacy.egg.DragonsLegacy#init}.</li>
 *   <li>Call {@link #init()} to load (or create) the config file.</li>
 *   <li>Call {@link #reload()} at any time (e.g. from {@code /dragonslegacy reload})
 *       to re-read the file and return a fresh {@link ConfigData}.</li>
 * </ol>
 *
 * <p>Missing or malformed fields fall back to the defaults defined in
 * {@link ConfigData} so the server never crashes due to a bad config.
 */
public class ConfigManager {

    private static final String FILE_NAME = "config.yml";

    private final Path configPath;
    private ConfigData data = new ConfigData();

    public ConfigManager() {
        this.configPath = DragonsLegacyMod.CONFIG_DIR.resolve(FILE_NAME);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Ensures the config directory and file exist, then loads the config.
     * If the file is absent a default one is written to disk.
     */
    public void init() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Could not create config directory.", e);
        }

        if (!configPath.toFile().isFile()) {
            data = new ConfigData();
            save();
            DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Created default config.yml at {}.", configPath);
            return;
        }

        reload();
    }

    /**
     * Re-reads {@code config.yml} from disk and returns the loaded data.
     * Falls back to the previous (or default) values on failure.
     */
    public ConfigData reload() {
        YamlConfigurationLoader loader = buildLoader();
        try {
            CommentedConfigurationNode node = loader.load();
            ConfigData loaded = node.get(ConfigData.class);
            if (loaded == null) {
                DragonsLegacyMod.LOGGER.warn(
                    "[Dragon's Legacy] config.yml appears empty or unreadable – using defaults.");
                data = new ConfigData();
            } else {
                mergeDefaults(loaded);
                data = loaded;
            }
            DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] config.yml loaded successfully.");
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Failed to load config.yml – keeping previous values.", e);
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the announcement template for {@code key} (e.g. {@code "egg_picked_up"}).
     * Falls back to the hardcoded default if the key is absent.
     */
    public String getAnnouncementTemplate(String key) {
        if (data.announcements != null) {
            String value = data.announcements.get(key);
            if (value != null) return value;
        }
        // Fall back to built-in defaults
        return ConfigData.defaultAnnouncements().getOrDefault(key, "");
    }

    /** Returns the configured Dragon's Hunger duration in ticks. */
    public int getAbilityDurationTicks() {
        return data.abilityDurationTicks;
    }

    /** Returns the configured Dragon's Hunger cooldown in ticks. */
    public int getAbilityCooldownTicks() {
        return data.abilityCooldownTicks;
    }

    /** Returns the number of real-world days before an offline bearer is cleared. */
    public double getOfflineResetDays() {
        return data.offlineResetDays;
    }

    /** Returns {@code true} if the egg spawn-fallback behaviour is enabled. */
    public boolean getSpawnFallbackEnabled() {
        return data.spawnFallbackEnabled;
    }

    /** Returns the full announcement templates map (never {@code null}). */
    public Map<String, String> getAnnouncementTemplates() {
        return data.announcements != null ? data.announcements : ConfigData.defaultAnnouncements();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void save() {
        YamlConfigurationLoader loader = buildLoader();
        try {
            CommentedConfigurationNode node = loader.createNode();
            node.set(ConfigData.class, data);
            loader.save(node);
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Failed to save config.yml.", e);
        }
    }

    private YamlConfigurationLoader buildLoader() {
        return YamlConfigurationLoader.builder()
            .path(configPath)
            .defaultOptions(opts -> opts.serializers(build ->
                build.registerAnnotatedObjects(ObjectMapper.factory())
            ))
            .build();
    }

    /**
     * Fills in any fields that were absent or invalid in {@code loaded} with
     * their defaults, so the caller never has to null-check every field.
     */
    private static void mergeDefaults(ConfigData loaded) {
        ConfigData defaults = new ConfigData();

        // Merge announcement templates – keep any user-defined key and add missing ones
        if (loaded.announcements == null) {
            loaded.announcements = defaults.announcements;
        } else {
            Map<String, String> merged = new LinkedHashMap<>(defaults.announcements);
            merged.putAll(loaded.announcements);
            loaded.announcements = merged;
        }

        if (loaded.abilityDurationTicks <= 0) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] config.yml: abilityDurationTicks must be > 0; using default {}.",
                defaults.abilityDurationTicks);
            loaded.abilityDurationTicks = defaults.abilityDurationTicks;
        }
        if (loaded.abilityCooldownTicks <= 0) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] config.yml: abilityCooldownTicks must be > 0; using default {}.",
                defaults.abilityCooldownTicks);
            loaded.abilityCooldownTicks = defaults.abilityCooldownTicks;
        }
        if (loaded.offlineResetDays <= 0) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] config.yml: offlineResetDays must be > 0; using default {}.",
                defaults.offlineResetDays);
            loaded.offlineResetDays = defaults.offlineResetDays;
        }
    }
}
