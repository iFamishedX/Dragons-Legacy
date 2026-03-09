package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Messages configuration loaded from {@code config/dragonslegacy/messages.yaml}.
 *
 * <p>Each message is a full entry with multi-channel routing, visibility rules,
 * per-player cooldowns, global cooldowns, ordering, and conditions.
 *
 * <p>Use {@link #getEntry(String)} for backward-compatible single-channel access,
 * or {@link #getChannels(String)} for full multi-channel dispatch.
 */
@ConfigSerializable
public class MessagesConfig {

    private static final NodeParser PARSER = NodeParser.builder()
        .globalPlaceholders()
        .quickText()
        .staticPreParsing()
        .build();

    private static final Set<String> VALID_MODES =
        Set.of("chat", "actionbar", "bossbar", "title", "subtitle");

    private static final Set<String> VALID_VISIBILITY =
        Set.of("bearer_only", "everyone", "everyone_except_bearer", "executor_only", "everyone_except_executor");

    // Per-player cooldown tracking: messageKey -> playerUUID -> last-send tick
    private final ConcurrentHashMap<String, ConcurrentHashMap<UUID, Long>> playerCooldowns =
        new ConcurrentHashMap<>();

    // Global cooldown tracking: messageKey -> last-send tick
    private final ConcurrentHashMap<String, Long> globalCooldowns = new ConcurrentHashMap<>();

    // =========================================================================
    // Nested types
    // =========================================================================

    /**
     * A single channel within a message entry.
     */
    @ConfigSerializable
    public static class ChannelEntry {
        public String mode = "chat";
        public String visibility = "everyone";
        public String text = "";

        private transient MessageString resolvedText;

        public MessageString getResolvedText() {
            if (resolvedText == null) {
                resolvedText = new MessageString(PARSER, text != null ? text : "");
            }
            return resolvedText;
        }
    }

    /**
     * A full message entry loaded from messages.yaml.
     */
    @ConfigSerializable
    public static class MessageConfig {
        public int order = 0;

        @Setting("cooldown_ticks")
        public int cooldownTicks = 0;

        @Setting("global_cooldown_ticks")
        public int globalCooldownTicks = 0;

        public Map<String, Boolean> conditions = new LinkedHashMap<>();

        public List<ChannelEntry> channels = new ArrayList<>();
    }

    /**
     * A resolved message entry - the primary backward-compatible type.
     * Points to the first channel's mode and text.
     */
    public static class MessageEntry {
        public final String output;
        public final MessageString text;
        public final List<ChannelEntry> channels;
        public final int cooldownTicks;
        public final int globalCooldownTicks;
        public final Map<String, Boolean> conditions;
        public final int order;

        public MessageEntry(String output, MessageString text, List<ChannelEntry> channels,
                            int cooldownTicks, int globalCooldownTicks,
                            Map<String, Boolean> conditions, int order) {
            this.output = output;
            this.text = text;
            this.channels = channels;
            this.cooldownTicks = cooldownTicks;
            this.globalCooldownTicks = globalCooldownTicks;
            this.conditions = conditions;
            this.order = order;
        }
    }

    // =========================================================================
    // Config fields
    // =========================================================================

    @Setting("config_version")
    public int configVersion = 1;

    /** Global prefix prepended to messages that include %dragonslegacy:global_prefix%. */
    public String prefix = "";

    /** Per-message configuration map, keyed by message identifier. */
    public Map<String, MessageConfig> messages = buildDefaultMessages();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Returns a backward-compatible {@link MessageEntry} for the given key.
     * Uses the first channel's mode and text as the primary output.
     * Logs a warning for missing keys; never throws.
     */
    public MessageEntry getEntry(String key) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' not found, using empty defaults.", key);
            cfg = new MessageConfig();
            cfg.channels = new ArrayList<>();
            ChannelEntry fallback = new ChannelEntry();
            fallback.mode = "chat";
            fallback.visibility = "everyone";
            fallback.text = "";
            cfg.channels.add(fallback);
        }

        List<ChannelEntry> channels = cfg.channels != null ? cfg.channels : new ArrayList<>();
        if (channels.isEmpty()) {
            ChannelEntry fallback = new ChannelEntry();
            fallback.mode = "chat";
            fallback.visibility = "everyone";
            fallback.text = "";
            channels = List.of(fallback);
        }

        ChannelEntry primary = channels.get(0);
        String mode = validateMode(key, primary.mode);
        validateVisibility(key, primary.visibility);

        return new MessageEntry(
            mode,
            primary.getResolvedText(),
            channels,
            cfg.cooldownTicks,
            cfg.globalCooldownTicks,
            cfg.conditions != null ? cfg.conditions : Map.of(),
            cfg.order
        );
    }

    /**
     * Returns all channels for the given key, or an empty list if not found.
     */
    public List<ChannelEntry> getChannels(String key) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null || cfg.channels == null) return List.of();
        return cfg.channels;
    }

    /**
     * Checks whether the per-player cooldown for a message key has elapsed.
     * If it has (or there is no cooldown), records the current time and returns true.
     */
    public boolean checkAndUpdatePlayerCooldown(String key, UUID playerUUID, long nowTick) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null || cfg.cooldownTicks <= 0) return true;

        ConcurrentHashMap<UUID, Long> map =
            playerCooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        Long lastSent = map.get(playerUUID);
        if (lastSent != null && (nowTick - lastSent) < cfg.cooldownTicks) return false;
        map.put(playerUUID, nowTick);
        return true;
    }

    /**
     * Checks whether the global cooldown for a message key has elapsed.
     * If it has (or there is no cooldown), records the current time and returns true.
     */
    public boolean checkAndUpdateGlobalCooldown(String key, long nowTick) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null || cfg.globalCooldownTicks <= 0) return true;

        Long lastSent = globalCooldowns.get(key);
        if (lastSent != null && (nowTick - lastSent) < cfg.globalCooldownTicks) return false;
        globalCooldowns.put(key, nowTick);
        return true;
    }

    // =========================================================================
    // Validation helpers
    // =========================================================================

    private String validateMode(String key, String mode) {
        if (mode == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' channel has null mode, defaulting to 'chat'.", key);
            return "chat";
        }
        String normalized = mode.toLowerCase(Locale.ROOT).trim();
        if (!VALID_MODES.contains(normalized)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' has unknown output mode '{}', defaulting to 'chat'.",
                key, mode);
            return "chat";
        }
        return normalized;
    }

    private void validateVisibility(String key, String visibility) {
        if (visibility == null) return;
        String normalized = visibility.toLowerCase(Locale.ROOT).trim();
        if (!VALID_VISIBILITY.contains(normalized)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' has unknown visibility '{}'. Allowed: {}.",
                key, visibility, VALID_VISIBILITY);
        }
    }

    // =========================================================================
    // Default messages
    // =========================================================================

    private static Map<String, MessageConfig> buildDefaultMessages() {
        Map<String, MessageConfig> map = new LinkedHashMap<>();

        map.put("help", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% <gold><bold>Dragon's Legacy Commands</bold></gold>\n/dragonslegacy help\n/dragonslegacy bearer\n/dragonslegacy hunger on\n/dragonslegacy hunger off"))));

        map.put("bearer_info", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% <yellow>The Dragon Egg is held by <gold>%dragonslegacy:bearer%</gold>.</yellow>"))));

        map.put("bearer_none", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% <yellow>No one holds the Dragon Egg yet.</yellow>"))));

        map.put("hunger_activate", buildEntry(0, 0, 0, Map.<String, Boolean>of("ability_active", Boolean.TRUE),
            List.of(channel("title", "bearer_only",
                "<#FF4500><bold>Dragon's Hunger!</bold>"))));

        map.put("hunger_deactivate", buildEntry(0, 0, 0, Map.<String, Boolean>of("ability_active", Boolean.FALSE),
            List.of(channel("title", "bearer_only",
                "<gray><italic>Dragon's Hunger fades...</italic></gray>"))));

        map.put("hunger_expired", buildEntry(0, 0, 0, Map.<String, Boolean>of("ability_active", Boolean.FALSE),
            List.of(channel("title", "bearer_only",
                "<gray><italic>Dragon's Hunger has ended.</italic></gray>"))));

        map.put("not_bearer", buildEntry(0, 20, 0, Map.<String, Boolean>of("executor_is_not_bearer", Boolean.TRUE),
            List.of(channel("actionbar", "executor_only",
                "<red>You are not the Dragon Egg bearer!</red>"))));

        map.put("elytra_blocked", buildEntry(0, 20, 0, Map.<String, Boolean>of("ability_active", Boolean.TRUE),
            List.of(channel("actionbar", "bearer_only",
                "<red>You cannot use an elytra while Dragon's Hunger is active!</red>"))));

        map.put("announcement_egg_picked_up", buildEntry(0, 0, 0, Map.<String, Boolean>of("egg_held", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% %dragonslegacy:player% picked up the Dragon Egg."))));

        map.put("announcement_egg_dropped", buildEntry(0, 0, 0, Map.<String, Boolean>of("egg_dropped", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% The Dragon Egg was dropped."))));

        map.put("announcement_egg_placed", buildEntry(0, 0, 0, Map.<String, Boolean>of("egg_placed", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% Dragon Egg placed at %dragonslegacy:x%, %dragonslegacy:y%, %dragonslegacy:z%."))));

        map.put("announcement_bearer_changed", buildEntry(0, 0, 0, Map.<String, Boolean>of("bearer_changed", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% New bearer: %dragonslegacy:bearer%."))));

        map.put("announcement_bearer_cleared", buildEntry(0, 0, 0, Map.<String, Boolean>of("bearer_changed", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% The Dragon Egg has no bearer."))));

        map.put("announcement_egg_teleported", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% Dragon Egg returned to spawn."))));

        map.put("announcement_ability_activated", buildEntry(0, 0, 0, Map.<String, Boolean>of("ability_active", Boolean.TRUE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% %dragonslegacy:player% activated Dragon's Hunger."))));

        map.put("announcement_ability_expired", buildEntry(0, 0, 0, Map.<String, Boolean>of("ability_active", Boolean.FALSE),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% Dragon's Hunger expired."))));

        map.put("announcement_ability_cooldown_started", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% Ability cooldown started."))));

        map.put("announcement_ability_cooldown_ended", buildEntry(0, 0, 0, Map.of(),
            List.of(channel("chat", "everyone",
                "%dragonslegacy:global_prefix% Ability ready."))));

        return map;
    }

    private static MessageConfig buildEntry(int order, int cooldownTicks, int globalCooldownTicks,
                                             Map<String, Boolean> conditions, List<ChannelEntry> channels) {
        MessageConfig cfg = new MessageConfig();
        cfg.order = order;
        cfg.cooldownTicks = cooldownTicks;
        cfg.globalCooldownTicks = globalCooldownTicks;
        cfg.conditions = new LinkedHashMap<>(conditions);
        cfg.channels = new ArrayList<>(channels);
        return cfg;
    }

    private static ChannelEntry channel(String mode, String visibility, String text) {
        ChannelEntry ch = new ChannelEntry();
        ch.mode = mode;
        ch.visibility = visibility;
        ch.text = text;
        return ch;
    }
}
