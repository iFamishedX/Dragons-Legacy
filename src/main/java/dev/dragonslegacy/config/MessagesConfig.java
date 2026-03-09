package dev.dragonslegacy.config;

import dev.dragonslegacy.DragonsLegacyMod;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Unified messages configuration loaded from {@code config/dragonslegacy/messages.yaml}.
 *
 * <p>Replaces the previous field-based MessagesConfig and AnnouncementsConfig.
 * Messages are stored in two flat maps:
 * <ul>
 *   <li>{@code output_modes} – maps message key → output mode string</li>
 *   <li>{@code text}         – maps message key → MiniMessage text</li>
 * </ul>
 * Use {@link #getEntry(String)} to obtain a {@link MessageEntry} for a given key.
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

    // -------------------------------------------------------------------------
    // Nested type
    // -------------------------------------------------------------------------

    /**
     * A resolved message entry with an output mode and parsed text.
     * Instances are created on-demand by {@link MessagesConfig#getEntry(String)}.
     */
    public static class MessageEntry {
        public final String output;
        public final MessageString text;

        public MessageEntry(String output, MessageString text) {
            this.output = output;
            this.text   = text;
        }
    }

    // -------------------------------------------------------------------------
    // Config fields
    // -------------------------------------------------------------------------

    @Setting("use_minimessage")
    public boolean useMiniMessage = true;

    @Setting("output_modes")
    public Map<String, String> outputModes = buildDefaultOutputModes();

    public Map<String, MessageString> text = buildDefaultText();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link MessageEntry} for the given key.
     * If the output mode is missing or invalid, defaults to {@code "chat"} with a warning.
     * If the text is missing, returns an empty string.
     *
     * @param key the message key (e.g. {@code "help"}, {@code "bearer_info"})
     * @return a non-null {@link MessageEntry}
     */
    public MessageEntry getEntry(String key) {
        // Resolve output mode
        String rawMode = (outputModes != null) ? outputModes.get(key) : null;
        String mode = "chat";
        if (rawMode != null) {
            String normalized = rawMode.toLowerCase(Locale.ROOT).trim();
            if (VALID_MODES.contains(normalized)) {
                mode = normalized;
            } else {
                DragonsLegacyMod.LOGGER.warn(
                    "[Dragon's Legacy] messages.yaml: key '{}' has unknown output mode '{}', defaulting to 'chat'.",
                    key, rawMode);
            }
        }
        // Resolve text
        MessageString ms = (text != null) ? text.get(key) : null;
        if (ms == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: text for key '{}' is missing, using empty string.", key);
            ms = new MessageString(PARSER, "");
        }
        return new MessageEntry(mode, ms);
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    private static Map<String, String> buildDefaultOutputModes() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("help",                              "chat");
        map.put("bearer_info",                       "chat");
        map.put("bearer_none",                       "chat");
        map.put("hunger_activate",                   "title");
        map.put("hunger_deactivate",                 "title");
        map.put("hunger_expired",                    "title");
        map.put("not_bearer",                        "actionbar");
        map.put("elytra_blocked",                    "actionbar");
        map.put("announcement_egg_picked_up",        "chat");
        map.put("announcement_egg_dropped",          "chat");
        map.put("announcement_egg_placed",           "chat");
        map.put("announcement_bearer_changed",       "chat");
        map.put("announcement_bearer_cleared",       "chat");
        map.put("announcement_egg_teleported",       "chat");
        map.put("announcement_ability_activated",    "chat");
        map.put("announcement_ability_expired",      "chat");
        map.put("announcement_ability_cooldown_started", "chat");
        map.put("announcement_ability_cooldown_ended",   "chat");
        return map;
    }

    private static Map<String, MessageString> buildDefaultText() {
        Map<String, MessageString> map = new LinkedHashMap<>();
        map.put("help",                new MessageString(PARSER,
            "\n/dragonslegacy help\n/dragonslegacy bearer\n/dragonslegacy hunger on\n/dragonslegacy hunger off"));
        map.put("bearer_info",         new MessageString(PARSER,
            "<yellow>The %deg:item% is held by <gold>%deg:bearer%</>."));
        map.put("bearer_none",         new MessageString(PARSER,
            "<yellow>No one holds the %deg:item% yet.</yellow>"));
        map.put("hunger_activate",     new MessageString(PARSER,
            "<#FF4500><bold>Dragon's Hunger!</bold>"));
        map.put("hunger_deactivate",   new MessageString(PARSER,
            "<gray><italic>Dragon's Hunger fades...</italic></gray>"));
        map.put("hunger_expired",      new MessageString(PARSER,
            "<gray><italic>Dragon's Hunger has ended.</italic></gray>"));
        map.put("not_bearer",          new MessageString(PARSER,
            "<red>You are not the Dragon Egg bearer!</red>"));
        map.put("elytra_blocked",      new MessageString(PARSER,
            "<red>You cannot use an elytra while Dragon's Hunger is active!</red>"));
        map.put("announcement_egg_picked_up",        new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> <player> picked up the egg."));
        map.put("announcement_egg_dropped",          new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> The egg was dropped."));
        map.put("announcement_egg_placed",           new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> Egg placed at <x>, <y>, <z>."));
        map.put("announcement_bearer_changed",       new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> New bearer: <player>."));
        map.put("announcement_bearer_cleared",       new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> The egg has no bearer."));
        map.put("announcement_egg_teleported",       new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> Egg returned to spawn."));
        map.put("announcement_ability_activated",    new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> <player> activated Dragon's Hunger."));
        map.put("announcement_ability_expired",      new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> Dragon's Hunger expired."));
        map.put("announcement_ability_cooldown_started", new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> Ability cooldown started."));
        map.put("announcement_ability_cooldown_ended",   new MessageString(PARSER,
            "<gold>[Dragon's Legacy]</gold> Ability ready."));
        return map;
    }
}
