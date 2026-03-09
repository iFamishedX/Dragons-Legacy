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
 *   <li>{@code config.yaml}   – master on/off switch and config version</li>
 *   <li>{@code egg.yaml}      – egg behavior, visibility, and protection settings</li>
 *   <li>{@code ability.yaml}  – Dragon's Hunger ability settings</li>
 *   <li>{@code passive.yaml}  – passive effects and attributes</li>
 *   <li>{@code glow.yaml}     – glow system settings</li>
 *   <li>{@code commands.yaml} – command names and aliases</li>
 *   <li>{@code messages.yaml} – all text output, output modes, and announcements</li>
 * </ul>
 */
public class ConfigManager {

    private CoreConfig         coreConfig     = new CoreConfig();
    private EggConfig          eggConfig      = new EggConfig();
    private AbilityFileConfig  abilityFile    = new AbilityFileConfig();
    private PassiveConfig      passiveConfig  = new PassiveConfig();
    private GlowConfig         glowConfig     = new GlowConfig();
    private CommandsFileConfig commandsFile   = new CommandsFileConfig();
    private MessagesConfig     messages       = new MessagesConfig();

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
        coreConfig    = loadOrCreate("config.yaml",   CoreConfig.class,        new CoreConfig());
        eggConfig     = loadOrCreate("egg.yaml",       EggConfig.class,         new EggConfig());
        abilityFile   = loadOrCreate("ability.yaml",   AbilityFileConfig.class, new AbilityFileConfig());
        passiveConfig = loadOrCreate("passive.yaml",   PassiveConfig.class,     new PassiveConfig());
        glowConfig    = loadOrCreate("glow.yaml",      GlowConfig.class,        new GlowConfig());
        commandsFile  = loadOrCreate("commands.yaml",  CommandsFileConfig.class,new CommandsFileConfig());
        messages      = loadOrCreate("messages.yaml",  MessagesConfig.class,    new MessagesConfig());
    }

    /**
     * Re-reads all YAML files from disk.
     */
    public void reload() {
        coreConfig    = reload("config.yaml",   CoreConfig.class,        coreConfig);
        eggConfig     = reload("egg.yaml",       EggConfig.class,         eggConfig);
        abilityFile   = reload("ability.yaml",   AbilityFileConfig.class, abilityFile);
        passiveConfig = reload("passive.yaml",   PassiveConfig.class,     passiveConfig);
        glowConfig    = reload("glow.yaml",      GlowConfig.class,        glowConfig);
        commandsFile  = reload("commands.yaml",  CommandsFileConfig.class,commandsFile);
        messages      = reload("messages.yaml",  MessagesConfig.class,    messages);
        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] All configuration files reloaded.");
    }

    // -------------------------------------------------------------------------
    // Primary getters (new API)
    // -------------------------------------------------------------------------

    public CoreConfig         getCore()         { return coreConfig; }
    public EggConfig          getEggConfig()    { return eggConfig; }
    public AbilityFileConfig  getAbilityFile()  { return abilityFile; }
    public PassiveConfig      getPassiveConfig(){ return passiveConfig; }
    public GlowConfig         getGlow()         { return glowConfig; }
    public CommandsFileConfig getCommandsFile() { return commandsFile; }
    public MessagesConfig     getMessages()     { return messages; }

    /** @deprecated Use {@link #getCore()} for the enabled flag. Builds a synthetic UnifiedConfig. */
    @Deprecated
    public UnifiedConfig getUnified() {
        UnifiedConfig u = new UnifiedConfig();
        u.egg      = toEggSection();
        u.ability  = toAbilitySection();
        u.passive  = toPassiveSection();
        u.commands = toCommandsSection();
        return u;
    }

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
        m.offlineResetDays    = eggConfig.offlineResetDays;
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
     * Returns a {@link CommandsConfig} built from commands.yaml.
     */
    public CommandsConfig getCommands() {
        CommandsConfig c = new CommandsConfig();
        if (commandsFile.root    != null) c.rootCommand = commandsFile.root;
        if (commandsFile.aliases != null) c.rootAliases = commandsFile.aliases;
        if (commandsFile.subcommands != null) {
            c.subcommands.help         = commandsFile.subcommands.help;
            c.subcommands.bearer       = commandsFile.subcommands.bearer;
            c.subcommands.hunger       = commandsFile.subcommands.hunger;
            c.subcommands.hungerOn     = commandsFile.subcommands.hungerOn;
            c.subcommands.hungerOff    = commandsFile.subcommands.hungerOff;
            c.subcommands.reload       = commandsFile.subcommands.reload;
            c.subcommands.test         = commandsFile.subcommands.test;
            c.subcommands.placeholders = commandsFile.subcommands.placeholders;
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
        MessagesConfig.MessageConfig cfg = (messages.messages != null) ? messages.messages.get(newKey) : null;
        if (cfg != null && cfg.channels != null && !cfg.channels.isEmpty()) {
            templates.put(oldKey, cfg.channels.get(0).text != null ? cfg.channels.get(0).text : "");
        } else {
            templates.put(oldKey, AnnouncementsConfig.defaults().getOrDefault(oldKey, ""));
        }
    }

    private UnifiedConfig.EggSection toEggSection() {
        UnifiedConfig.EggSection s = new UnifiedConfig.EggSection();
        s.searchRadius        = eggConfig.searchRadius;
        s.blockEnderChest     = eggConfig.blockEnderChest;
        s.blockContainerItems = eggConfig.blockContainerItems;
        s.offlineResetDays    = eggConfig.offlineResetDays;
        s.nearbyRange         = eggConfig.nearbyRange;
        if (eggConfig.visibility != null) s.visibility = eggConfig.visibility;
        return s;
    }

    private UnifiedConfig.AbilitySection toAbilitySection() {
        UnifiedConfig.AbilitySection s = new UnifiedConfig.AbilitySection();
        s.durationTicks = abilityFile.durationTicks;
        s.cooldownTicks = abilityFile.cooldownTicks;
        s.blockElytra   = abilityFile.blockElytra;
        if (abilityFile.effects    != null) s.effects    = abilityFile.effects;
        if (abilityFile.attributes != null) s.attributes = abilityFile.attributes;
        return s;
    }

    private UnifiedConfig.PassiveSection toPassiveSection() {
        UnifiedConfig.PassiveSection s = new UnifiedConfig.PassiveSection();
        if (passiveConfig.effects    != null) s.effects    = passiveConfig.effects;
        if (passiveConfig.attributes != null) s.attributes = passiveConfig.attributes;
        return s;
    }

    private UnifiedConfig.CommandsSection toCommandsSection() {
        UnifiedConfig.CommandsSection s = new UnifiedConfig.CommandsSection();
        if (commandsFile.root    != null) s.root    = commandsFile.root;
        if (commandsFile.aliases != null) s.aliases = new ArrayList<>(commandsFile.aliases);
        if (commandsFile.subcommands != null) {
            s.subcommands.help         = commandsFile.subcommands.help;
            s.subcommands.bearer       = commandsFile.subcommands.bearer;
            s.subcommands.hunger       = commandsFile.subcommands.hunger;
            s.subcommands.hungerOn     = commandsFile.subcommands.hungerOn;
            s.subcommands.hungerOff    = commandsFile.subcommands.hungerOff;
            s.subcommands.reload       = commandsFile.subcommands.reload;
            s.subcommands.test         = commandsFile.subcommands.test;
            s.subcommands.placeholders = commandsFile.subcommands.placeholders;
        }
        return s;
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
