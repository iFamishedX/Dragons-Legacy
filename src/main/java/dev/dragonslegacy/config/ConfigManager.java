package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads, saves, and reloads Dragon's Legacy configuration files
 * under {@code config/dragonslegacy/}.
 *
 * <h3>Files managed</h3>
 * <ul>
 *   <li>{@code config.yaml}   – unified egg, ability, passive, and commands settings</li>
 *   <li>{@code messages.yaml} – all text output, output modes, and announcements</li>
 * </ul>
 */
public class ConfigManager {

    private UnifiedConfig unified  = new UnifiedConfig();
    private MessagesConfig messages = new MessagesConfig();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Ensures the config directory exists and loads all files (writing defaults if absent).
     */
    public void init() {
        try {
            Files.createDirectories(DragonsLegacyMod.CONFIG_DIR);
        } catch (IOException e) {
            DragonsLegacyMod.LOGGER.warn("[Dragon's Legacy] Could not create config directory.", e);
        }
        unified  = loadOrCreate("config.yaml",   UnifiedConfig.class,  new UnifiedConfig());
        messages = loadOrCreate("messages.yaml",  MessagesConfig.class, new MessagesConfig());
    }

    /**
     * Re-reads all YAML files from disk.
     */
    public void reload() {
        unified  = reload("config.yaml",   UnifiedConfig.class,  unified);
        messages = reload("messages.yaml",  MessagesConfig.class, messages);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] All configuration files reloaded.");
    }

    // -------------------------------------------------------------------------
    // Primary getters
    // -------------------------------------------------------------------------

    public UnifiedConfig  getUnified()  { return unified; }
    public MessagesConfig getMessages() { return messages; }

    // -------------------------------------------------------------------------
    // Adapter getters (backward-compatible with legacy code)
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link MainConfig} built from the {@code egg} section of config.yaml.
     */
    public MainConfig getMain() {
        MainConfig m = new MainConfig();
        UnifiedConfig.EggSection egg = unified.egg;
        if (egg != null) {
            m.searchRadius        = egg.searchRadius;
            m.blockEnderChest     = egg.blockEnderChest;
            m.blockContainerItems = egg.blockContainerItems;
            m.offlineResetDays    = egg.offlineResetDays;
            m.nearbyRange         = egg.nearbyRange;
            if (egg.visibility != null) m.visibility = egg.visibility;
        }
        return m;
    }

    /**
     * Returns an {@link AbilityConfig} built from the {@code ability} section of config.yaml.
     */
    public AbilityConfig getAbility() {
        AbilityConfig a = new AbilityConfig();
        UnifiedConfig.AbilitySection ab = unified.ability;
        if (ab != null) {
            a.durationTicks = ab.durationTicks;
            a.cooldownTicks = ab.cooldownTicks;
            a.blockElytra   = ab.blockElytra;
            if (ab.effects    != null) a.effects    = ab.effects;
            if (ab.attributes != null) a.attributes = ab.attributes;
        }
        return a;
    }

    /**
     * Returns a {@link PassiveEffectsConfig} built from the {@code passive} section of config.yaml.
     */
    public PassiveEffectsConfig getPassiveEffects() {
        PassiveEffectsConfig p = new PassiveEffectsConfig();
        UnifiedConfig.PassiveSection pas = unified.passive;
        if (pas != null) {
            if (pas.effects    != null) p.effects    = pas.effects;
            if (pas.attributes != null) p.attributes = pas.attributes;
        }
        return p;
    }

    /**
     * Returns a {@link SpawnConfig} with defaults (BlueMap removed; fallback always enabled).
     */
    public SpawnConfig getSpawn() {
        SpawnConfig s = new SpawnConfig();
        s.fallbackEnabled = true;
        return s;
    }

    /**
     * Returns a {@link CommandsConfig} built from the {@code commands} section of config.yaml.
     */
    public CommandsConfig getCommands() {
        CommandsConfig c = new CommandsConfig();
        UnifiedConfig.CommandsSection cmd = unified.commands;
        if (cmd != null) {
            if (cmd.root    != null) c.rootCommand  = cmd.root;
            if (cmd.aliases != null) c.rootAliases  = cmd.aliases;
            if (cmd.subcommands != null) {
                c.subcommands.help     = cmd.subcommands.help;
                c.subcommands.bearer   = cmd.subcommands.bearer;
                c.subcommands.hunger   = cmd.subcommands.hunger;
                c.subcommands.hungerOn = cmd.subcommands.hungerOn;
                c.subcommands.hungerOff= cmd.subcommands.hungerOff;
            }
        }
        return c;
    }

    /**
     * Returns an {@link AnnouncementsConfig} built from the announcement entries in messages.yaml.
     * The templates map is populated using keys prefixed with {@code announcement_}.
     */
    public AnnouncementsConfig getAnnouncements() {
        AnnouncementsConfig a = new AnnouncementsConfig();
        a.useMiniMessage = messages.useMiniMessage;
        Map<String, String> templates = new LinkedHashMap<>();
        addTemplate(templates, "egg_picked_up",            "announcement_egg_picked_up");
        addTemplate(templates, "egg_dropped",              "announcement_egg_dropped");
        addTemplate(templates, "egg_placed",               "announcement_egg_placed");
        addTemplate(templates, "bearer_changed",           "announcement_bearer_changed");
        addTemplate(templates, "bearer_cleared",           "announcement_bearer_cleared");
        addTemplate(templates, "egg_teleported_to_spawn",  "announcement_egg_teleported");
        addTemplate(templates, "ability_activated",        "announcement_ability_activated");
        addTemplate(templates, "ability_expired",          "announcement_ability_expired");
        addTemplate(templates, "ability_cooldown_started", "announcement_ability_cooldown_started");
        addTemplate(templates, "ability_cooldown_ended",   "announcement_ability_cooldown_ended");
        a.templates = templates;
        return a;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addTemplate(Map<String, String> templates, String oldKey, String newKey) {
        MessageString ms = (messages.text != null) ? messages.text.get(newKey) : null;
        if (ms != null) {
            templates.put(oldKey, ms.value);
        } else {
            templates.put(oldKey, AnnouncementsConfig.defaults().getOrDefault(oldKey, ""));
        }
    }

    private <T> T loadOrCreate(String fileName, Class<T> type, T defaults) {
        Path path = DragonsLegacyMod.CONFIG_DIR.resolve(fileName);
        if (!path.toFile().isFile()) {
            boolean copied = copyDefaultResource(fileName, path);
            if (copied) {
                DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] Created default {} at {}.", fileName, path);
            }
        }
        return reload(fileName, type, defaults);
    }

    private <T> T reload(String fileName, Class<T> type, T previous) {
        Path path = DragonsLegacyMod.CONFIG_DIR.resolve(fileName);
        YamlConfigurationLoader loader = buildLoader(path);
        try {
            CommentedConfigurationNode node = loader.load();
            T loaded = node.get(type);
            if (loaded == null) {
                DragonsLegacyMod.LOGGER.warn(
                    "[Dragon's Legacy] {} appears empty – using defaults.", fileName);
                return previous;
            }
            DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] {} loaded.", fileName);
            return loaded;
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Failed to load {} – keeping previous values.", fileName, e);
            return previous;
        }
    }

    private boolean copyDefaultResource(String fileName, Path dest) {
        String resourcePath = "defaults/dragonslegacy/" + fileName;
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Could not copy default resource {} – file will not be created.", fileName, e);
            return false;
        }
        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Bundled default resource '{}' not found; config file will not be created.", resourcePath);
        return false;
    }

    private static YamlConfigurationLoader buildLoader(Path path) {
        return YamlConfigurationLoader.builder()
            .path(path)
            .defaultOptions(opts -> opts.serializers(build -> {
                build.registerAnnotatedObjects(ObjectMapper.factory());
                build.register(MessageString.class, new MessageString.Serializer(
                    NodeParser.builder()
                        .globalPlaceholders()
                        .quickText()
                        .staticPreParsing()
                        .build()
                ));
                build.register(Action.class, Action.Serializer.INSTANCE);
                build.register(CommandTemplate.class, CommandTemplate.Serializer.INSTANCE);
                build.register(Condition.class, Condition.Serializer.INSTANCE);
            }))
            .build();
    }
}
