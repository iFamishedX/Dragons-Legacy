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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads, saves, and reloads Dragon's Legacy configuration files
 * under {@code config/dragonslegacy/}.
 *
 * <h3>Files managed</h3>
 * <ul>
 *   <li>{@code global.yaml}       – permissions API toggle, command names/aliases, per-command permission nodes</li>
 *   <li>{@code egg.yaml}          – egg behavior, visibility, and protection settings</li>
 *   <li>{@code ability.yaml}      – Dragon's Hunger ability settings</li>
 *   <li>{@code passive.yaml}      – passive effects and attributes</li>
 *   <li>{@code infusion.yaml}     – infusion color and anvil material settings</li>
 *   <li>{@code messages.yaml}     – all text output, output modes, and announcements</li>
 *   <li>{@code logging.yaml}      – which log categories go to console / ops</li>
 *   <li>{@code placeholders.yaml} – config-driven external placeholder definitions</li>
 * </ul>
 */
public class ConfigManager {

    private GlobalConfig        globalConfig        = new GlobalConfig();
    private EggConfig           eggConfig           = new EggConfig();
    private AbilityFileConfig   abilityFile         = new AbilityFileConfig();
    private PassiveConfig       passiveConfig       = new PassiveConfig();
    private InfusionConfig      infusionConfig      = new InfusionConfig();
    private MessagesConfig      messages            = new MessagesConfig();
    private LoggingConfig       loggingConfig       = new LoggingConfig();
    private PlaceholdersConfig  placeholdersConfig  = new PlaceholdersConfig();

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
        globalConfig       = loadOrCreate("global.yaml",       GlobalConfig.class,       new GlobalConfig());
        eggConfig          = loadOrCreate("egg.yaml",           EggConfig.class,          new EggConfig());
        abilityFile        = loadOrCreate("ability.yaml",       AbilityFileConfig.class,  new AbilityFileConfig());
        passiveConfig      = loadOrCreate("passive.yaml",       PassiveConfig.class,      new PassiveConfig());
        infusionConfig     = loadOrCreate("infusion.yaml",      InfusionConfig.class,     new InfusionConfig());
        messages           = loadOrCreate("messages.yaml",      MessagesConfig.class,     new MessagesConfig());
        loggingConfig      = loadOrCreate("logging.yaml",       LoggingConfig.class,      new LoggingConfig());
        placeholdersConfig = loadOrCreate("placeholders.yaml",  PlaceholdersConfig.class, new PlaceholdersConfig());
    }

    /**
     * Re-reads all YAML files from disk.
     */
    public void reload() {
        globalConfig       = reload("global.yaml",       GlobalConfig.class,       globalConfig);
        eggConfig          = reload("egg.yaml",           EggConfig.class,          eggConfig);
        abilityFile        = reload("ability.yaml",       AbilityFileConfig.class,  abilityFile);
        passiveConfig      = reload("passive.yaml",       PassiveConfig.class,      passiveConfig);
        infusionConfig     = reload("infusion.yaml",      InfusionConfig.class,     infusionConfig);
        messages           = reload("messages.yaml",      MessagesConfig.class,     messages);
        loggingConfig      = reload("logging.yaml",       LoggingConfig.class,      loggingConfig);
        placeholdersConfig = reload("placeholders.yaml",  PlaceholdersConfig.class, placeholdersConfig);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] All configuration files reloaded.");
    }

    // -------------------------------------------------------------------------
    // Primary getters (new API)
    // -------------------------------------------------------------------------

    public GlobalConfig        getGlobal()         { return globalConfig; }
    public EggConfig           getEggConfig()      { return eggConfig; }
    public AbilityFileConfig   getAbilityFile()    { return abilityFile; }
    public PassiveConfig       getPassiveConfig()  { return passiveConfig; }
    public InfusionConfig      getInfusion()       { return infusionConfig; }
    public MessagesConfig      getMessages()       { return messages; }
    public LoggingConfig       getLogging()        { return loggingConfig; }
    public PlaceholdersConfig  getPlaceholders()   { return placeholdersConfig; }

    // -------------------------------------------------------------------------
    // Adapter getters (backward-compatible with legacy code)
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link MainConfig} built from egg.yaml.
     */
    public MainConfig getMain() {
        MainConfig m = new MainConfig();
        m.searchRadius        = eggConfig.searchRadius;
        m.blockEnderChest     = eggConfig.blockEnderChest;
        m.blockContainerItems = eggConfig.blockContainerItems;
        m.nearbyRange         = eggConfig.nearbyRange;
        if (eggConfig.visibility != null) m.visibility = eggConfig.visibility;
        return m;
    }

    /**
     * Returns an {@link AbilityConfig} built from ability.yaml.
     */
    public AbilityConfig getAbility() {
        AbilityConfig a = new AbilityConfig();
        a.durationTicks = abilityFile.durationTicks;
        a.cooldownTicks = abilityFile.cooldownTicks;
        a.blockElytra   = abilityFile.blockElytra;
        if (abilityFile.effects    != null) a.effects    = abilityFile.effects;
        if (abilityFile.attributes != null) a.attributes = abilityFile.attributes;
        return a;
    }

    /**
     * Returns a {@link PassiveEffectsConfig} built from passive.yaml.
     */
    public PassiveEffectsConfig getPassiveEffects() {
        PassiveEffectsConfig p = new PassiveEffectsConfig();
        if (passiveConfig.effects    != null) p.effects    = passiveConfig.effects;
        if (passiveConfig.attributes != null) p.attributes = passiveConfig.attributes;
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
     * Returns a {@link CommandsConfig} built from global.yaml.
     * The {@code actions} list is always empty (actions are not configurable via this file).
     */
    public CommandsConfig getCommands() {
        CommandsConfig c = new CommandsConfig();
        GlobalConfig.CommandsSection cmds = globalConfig.commands;
        if (cmds != null) {
            if (cmds.root    != null) c.rootCommand = cmds.root;
            if (cmds.aliases != null) c.rootAliases = new ArrayList<>(cmds.aliases);
        }
        return c;
    }

    /**
     * Returns an {@link AnnouncementsConfig} built from the announcement entries in messages.yaml.
     * MiniMessage is always enabled.
     */
    public AnnouncementsConfig getAnnouncements() {
        AnnouncementsConfig a = new AnnouncementsConfig();
        a.useMiniMessage = true;
        Map<String, String> templates = new LinkedHashMap<>();
        addTemplate(templates, "egg_picked_up",           "egg_picked_up");
        addTemplate(templates, "egg_dropped",             "egg_dropped");
        addTemplate(templates, "egg_placed",              "egg_placed");
        addTemplate(templates, "bearer_changed",          "bearer_changed");
        addTemplate(templates, "bearer_cleared",          "bearer_cleared");
        addTemplate(templates, "egg_teleported_to_spawn", "egg_teleported");
        addTemplate(templates, "ability_activated",       "ability_activated");
        addTemplate(templates, "ability_expired",         "ability_expired");
        addTemplate(templates, "ability_cooldown_started","ability_cooldown_started");
        addTemplate(templates, "ability_cooldown_ended",  "ability_cooldown_ended");
        a.templates = templates;
        return a;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addTemplate(Map<String, String> templates, String templateKey, String messageKey) {
        MessagesConfig.MessageConfig cfg = (messages.messages != null) ? messages.messages.get(messageKey) : null;
        if (cfg != null && cfg.channels != null && !cfg.channels.isEmpty()) {
            templates.put(templateKey, cfg.channels.get(0).text != null ? cfg.channels.get(0).text : "");
        } else {
            templates.put(templateKey, AnnouncementsConfig.defaults().getOrDefault(templateKey, ""));
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
