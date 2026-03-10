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
 *
 * <h3>Channel structure (new format)</h3>
 * <pre>{@code
 * channels:
 *   channel_name:
 *     mode:     chat | actionbar | title | subtitle | bossbar
 *     audience: everyone | bearer_only | everyone_except_bearer |
 *               executor_only | everyone_except_executor
 *     priority: integer (higher fires first, default 0)
 *     delay:    ticks to wait before sending (default 0)
 *     text:     "<MiniMessage text>"
 * }</pre>
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

    private static final Set<String> VALID_AUDIENCE =
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
     * A single named channel within a message entry.
     */
    @ConfigSerializable
    public static class ChannelEntry {
        public String mode = "chat";

        /**
         * Who receives this channel. Accepts {@code audience} (new) or {@code visibility}
         * (legacy) — {@code audience} takes precedence when both are present.
         */
        public String audience = "everyone";

        /**
         * Legacy alias for {@code audience}. Kept for backward compatibility with
         * configs that were generated before the rename.
         */
        public String visibility = "";

        /** Higher-priority channels within the same message are sent first. */
        public int priority = 0;

        /** Ticks to wait before dispatching this channel (0 = immediate). */
        public int delay = 0;

        public String text = "";

        private transient MessageString resolvedText;

        /**
         * Returns the effective audience string (lower-cased and trimmed), preferring
         * {@code audience} over the legacy {@code visibility} field.
         */
        public String getAudience() {
            if (audience != null && !audience.isBlank()) return audience.toLowerCase(Locale.ROOT).trim();
            if (visibility != null && !visibility.isBlank()) return visibility.toLowerCase(Locale.ROOT).trim();
            return "everyone";
        }

        public MessageString getResolvedText() {
            if (resolvedText == null) {
                resolvedText = new MessageString(PARSER, text != null ? text : "");
            }
            return resolvedText;
        }
    }

    /**
     * A full message entry loaded from messages.yaml.
     *
     * <p>Channels are now stored as a named map ({@code Map<String, ChannelEntry>})
     * instead of a plain list, so each channel has a human-readable identifier.
     * Iteration order follows insertion order (LinkedHashMap).
     */
    @ConfigSerializable
    public static class MessageConfig {
        /**
         * When {@code true} (and {@code disabled} is {@code false}), this message will be sent.
         * Set to {@code false} to silence this message regardless of conditions.
         */
        public boolean enabled = true;

        /**
         * Legacy field — kept for backward compatibility.
         * If {@code true}, overrides {@code enabled} and suppresses the message.
         */
        public boolean disabled = false;

        @Setting("cooldown_ticks")
        public int cooldownTicks = 0;

        @Setting("global_cooldown_ticks")
        public int globalCooldownTicks = 0;

        public Map<String, Boolean> conditions = new LinkedHashMap<>();

        /** Named channel map — keys are human-readable channel names. */
        public Map<String, ChannelEntry> channels = new LinkedHashMap<>();

        /** Returns {@code true} when the message should actually be sent. */
        public boolean isEnabled() {
            return enabled && !disabled;
        }
    }

    /** Singleton disabled entry returned for any disabled or missing message. */
    private static final MessageEntry DISABLED_ENTRY;

    static {
        ChannelEntry empty = new ChannelEntry();
        empty.mode = "chat";
        empty.audience = "everyone";
        empty.text = "";
        DISABLED_ENTRY = new MessageEntry("chat", empty.getResolvedText(),
            List.of(empty), 0, 0, Map.of(), true);
    }

    /**
     * A resolved message entry — the primary backward-compatible type.
     * Points to the highest-priority channel's mode and text.
     */
    public static class MessageEntry {
        public final String output;
        public final MessageString text;
        public final List<ChannelEntry> channels;
        public final int cooldownTicks;
        public final int globalCooldownTicks;
        public final Map<String, Boolean> conditions;
        public final boolean disabled;

        public MessageEntry(String output, MessageString text, List<ChannelEntry> channels,
                            int cooldownTicks, int globalCooldownTicks,
                            Map<String, Boolean> conditions, boolean disabled) {
            this.output = output;
            this.text = text;
            this.channels = channels;
            this.cooldownTicks = cooldownTicks;
            this.globalCooldownTicks = globalCooldownTicks;
            this.conditions = conditions;
            this.disabled = disabled;
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
     * Uses the highest-priority channel's mode and text as the primary output.
     * Returns a disabled entry if the message is disabled or missing.
     * Logs a warning for missing keys; never throws.
     */
    public MessageEntry getEntry(String key) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' not found, using empty defaults.", key);
            cfg = new MessageConfig();
            ChannelEntry fallback = new ChannelEntry();
            fallback.mode = "chat";
            fallback.audience = "everyone";
            fallback.text = "";
            cfg.channels = new LinkedHashMap<>();
            cfg.channels.put("main", fallback);
        }

        if (!cfg.isEnabled()) {
            return DISABLED_ENTRY;
        }

        // Build a list of channels sorted by descending priority
        List<ChannelEntry> sortedChannels = channelList(cfg);
        if (sortedChannels.isEmpty()) {
            ChannelEntry fallback = new ChannelEntry();
            fallback.mode = "chat";
            fallback.audience = "everyone";
            fallback.text = "";
            sortedChannels = List.of(fallback);
        }

        ChannelEntry primary = sortedChannels.get(0);
        String mode = validateMode(key, primary.mode);
        validateAudience(key, primary.getAudience());

        return new MessageEntry(
            mode,
            primary.getResolvedText(),
            sortedChannels,
            cfg.cooldownTicks,
            cfg.globalCooldownTicks,
            cfg.conditions != null ? cfg.conditions : Map.of(),
            false
        );
    }

    /**
     * Returns all channels for the given key as an ordered list (sorted by descending priority),
     * or an empty list if not found.
     */
    public List<ChannelEntry> getChannels(String key) {
        MessageConfig cfg = (messages != null) ? messages.get(key) : null;
        if (cfg == null || cfg.channels == null) return List.of();
        return channelList(cfg);
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
    // Private helpers
    // =========================================================================

    /**
     * Returns the channels of {@code cfg} as a list sorted by descending priority.
     */
    private static List<ChannelEntry> channelList(MessageConfig cfg) {
        if (cfg.channels == null || cfg.channels.isEmpty()) return new ArrayList<>();
        List<ChannelEntry> list = new ArrayList<>(cfg.channels.values());
        list.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return list;
    }

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

    private void validateAudience(String key, String audience) {
        if (audience == null) return;
        String normalized = audience.toLowerCase(Locale.ROOT).trim();
        if (!VALID_AUDIENCE.contains(normalized)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: key '{}' has unknown audience '{}'. Allowed: {}.",
                key, audience, VALID_AUDIENCE);
        }
    }

    // =========================================================================
    // Default messages
    // =========================================================================

    private static Map<String, MessageConfig> buildDefaultMessages() {
        Map<String, MessageConfig> map = new LinkedHashMap<>();

        map.put("help", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% <gold><bold>Dragon's Legacy Commands</bold></gold>\n/dragonslegacy help\n/dragonslegacy bearer\n/dragonslegacy hunger on\n/dragonslegacy hunger off"))));

        map.put("bearer_info", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% <yellow>The Dragon Egg is held by <gold>%dragonslegacy:bearer%</gold>.</yellow>"))));

        map.put("bearer_none", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% <yellow>No one holds the Dragon Egg yet.</yellow>"))));

        Map<String, ChannelEntry> activatedChannels = new LinkedHashMap<>();
        activatedChannels.put("title",     ch("title", "bearer_only", 10, 0, "<#FF4500><bold>Dragon's Hunger!</bold>"));
        activatedChannels.put("broadcast", ch("chat",  "everyone",     0, 0, "%dragonslegacy:global_prefix% %dragonslegacy:player% activated Dragon's Hunger."));
        map.put("ability_activated", buildEntry(0, 0, Map.<String, Boolean>of("ability_active", Boolean.TRUE), activatedChannels));

        map.put("ability_deactivated", buildEntry(0, 0, Map.<String, Boolean>of("ability_active", Boolean.FALSE),
            mapOf("title", ch("title", "bearer_only", 0, 0,
                "<gray><italic>Dragon's Hunger fades...</italic></gray>"))));

        Map<String, ChannelEntry> expiredChannels = new LinkedHashMap<>();
        expiredChannels.put("title",     ch("title", "bearer_only", 10, 0, "<gray><italic>Dragon's Hunger has ended.</italic></gray>"));
        expiredChannels.put("broadcast", ch("chat",  "everyone",     0, 0, "%dragonslegacy:global_prefix% Dragon's Hunger expired."));
        map.put("ability_expired", buildEntry(0, 0, Map.<String, Boolean>of("ability_active", Boolean.FALSE), expiredChannels));

        map.put("ability_cooldown_started", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% Ability cooldown started."))));

        map.put("ability_cooldown_ended", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% Ability ready."))));

        map.put("not_bearer", buildEntry(20, 0, Map.<String, Boolean>of("executor_is_not_bearer", Boolean.TRUE),
            mapOf("main", ch("actionbar", "executor_only", 0, 0,
                "<red>You are not the Dragon Egg bearer!</red>"))));

        map.put("elytra_blocked", buildEntry(20, 0, Map.<String, Boolean>of("ability_active", Boolean.TRUE),
            mapOf("main", ch("actionbar", "bearer_only", 0, 0,
                "<red>You cannot use an elytra while Dragon's Hunger is active!</red>"))));

        map.put("egg_picked_up", buildEntry(0, 0, Map.<String, Boolean>of("egg_held", Boolean.TRUE),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% %dragonslegacy:player% picked up the Dragon Egg."))));

        map.put("egg_dropped", buildEntry(0, 0, Map.<String, Boolean>of("egg_dropped", Boolean.TRUE),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% The Dragon Egg was dropped."))));

        map.put("egg_placed", buildEntry(0, 0, Map.<String, Boolean>of("egg_placed", Boolean.TRUE),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% Dragon Egg placed at %dragonslegacy:exact-xyz%."))));

        map.put("egg_teleported", buildEntry(0, 0, Map.of(),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% Dragon Egg returned to spawn."))));

        map.put("bearer_changed", buildEntry(0, 0, Map.<String, Boolean>of("bearer_changed", Boolean.TRUE),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% New bearer: %dragonslegacy:bearer%."))));

        map.put("bearer_cleared", buildEntry(0, 0, Map.<String, Boolean>of("bearer_changed", Boolean.TRUE),
            mapOf("main", ch("chat", "everyone", 0, 0,
                "%dragonslegacy:global_prefix% The Dragon Egg has no bearer."))));

        return map;
    }

    private static MessageConfig buildEntry(int cooldownTicks, int globalCooldownTicks,
                                             Map<String, Boolean> conditions,
                                             Map<String, ChannelEntry> channelMap) {
        MessageConfig cfg = new MessageConfig();
        cfg.enabled = true;
        cfg.cooldownTicks = cooldownTicks;
        cfg.globalCooldownTicks = globalCooldownTicks;
        cfg.conditions = new LinkedHashMap<>(conditions);
        cfg.channels = channelMap;
        return cfg;
    }

    /** Creates a single-channel named map. */
    private static Map<String, ChannelEntry> mapOf(String name, ChannelEntry entry) {
        Map<String, ChannelEntry> map = new LinkedHashMap<>();
        map.put(name, entry);
        return map;
    }

    private static ChannelEntry ch(String mode, String audience, int priority, int delay, String text) {
        ChannelEntry ch = new ChannelEntry();
        ch.mode = mode;
        ch.audience = audience;
        ch.priority = priority;
        ch.delay = delay;
        ch.text = text;
        return ch;
    }
}
