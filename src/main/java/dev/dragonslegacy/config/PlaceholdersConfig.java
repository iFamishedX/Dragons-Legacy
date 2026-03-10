package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholders configuration loaded from {@code config/dragonslegacy/placeholders.yaml}.
 *
 * <p>Each entry under {@code placeholders:} defines one external placeholder exposed as
 * {@code %dragonslegacy:name%}.  Internal placeholders ({x}, {state}, {bearer}, etc.) are
 * NOT registered with PlaceholderAPI; they are resolved at render time by
 * {@link dev.dragonslegacy.PlaceholderEngine}.
 *
 * <h3>Placeholder definition structure</h3>
 * <pre>{@code
 * placeholder_name:
 *   ignore_visibility: false    # true = always expose exact values (e.g. for debug)
 *   conditions:                 # evaluated top-to-bottom; first match wins
 *     - if: "{state} == 'HIDDEN'"
 *       output: "Hidden"
 *     - if: "{state} == 'PLAYER'"
 *       output: "Carried by {bearer}"
 *   format: "{round({x},50)}, {round({z},50)}"   # fallback when no condition matches
 * }</pre>
 */
@ConfigSerializable
public class PlaceholdersConfig {

    // =========================================================================
    // Nested types
    // =========================================================================

    /**
     * A single condition entry: if the {@code condition} expression evaluates to {@code true},
     * the placeholder returns {@code output}.
     */
    @ConfigSerializable
    public static class ConditionEntry {

        /** Boolean expression using internal placeholders, e.g. {@code "{state} == 'HIDDEN'"}. */
        @Setting("if")
        public String condition = "";

        /** Template string returned when the condition is true. Internal placeholders are resolved. */
        public String output = "";

        public ConditionEntry() {}

        public ConditionEntry(String condition, String output) {
            this.condition = condition;
            this.output = output;
        }
    }

    /**
     * A full placeholder definition.
     */
    @ConfigSerializable
    public static class PlaceholderDef {

        /**
         * When {@code true}, exact egg coordinates and real state are always exposed,
         * bypassing the visibility rules from {@code egg.yaml}.
         * Set to {@code true} for debug or admin-only placeholders.
         */
        @Setting("ignore_visibility")
        public boolean ignoreVisibility = false;

        /**
         * Optional list of conditions evaluated top-to-bottom.
         * The first condition whose {@code if} expression is true determines the output.
         * If no condition matches, {@link #format} is used as the fallback.
         */
        public List<ConditionEntry> conditions = new ArrayList<>();

        /**
         * Fallback template string used when no condition matches.
         * Supports internal placeholder tokens ({x}, {bearer}, etc.) and filters
         * (round, upper, lower, abs, etc.).
         */
        public String format = "";

        public PlaceholderDef() {}

        public PlaceholderDef(boolean ignoreVisibility, List<ConditionEntry> conditions, String format) {
            this.ignoreVisibility = ignoreVisibility;
            this.conditions = conditions;
            this.format = format;
        }
    }

    // =========================================================================
    // Config fields
    // =========================================================================

    @Setting("config_version")
    public int configVersion = 1;

    /**
     * Map of placeholder name → definition.  Each entry becomes one
     * {@code %dragonslegacy:name%} PlaceholderAPI entry.
     */
    public Map<String, PlaceholderDef> placeholders = buildDefaults();

    // =========================================================================
    // Defaults
    // =========================================================================

    private static Map<String, PlaceholderDef> buildDefaults() {
        Map<String, PlaceholderDef> map = new LinkedHashMap<>();

        // pretty_location – visibility-aware location summary
        map.put("pretty_location", new PlaceholderDef(
            false,
            List.of(
                new ConditionEntry("{state} == 'HIDDEN'", "Hidden"),
                new ConditionEntry("{state} == 'PLAYER'", "Carried by {bearer}")
            ),
            "{round({x},50)}, {round({z},50)}"
        ));

        // exact-xz – always show exact X, Z (for admins / debug)
        map.put("exact-xz", new PlaceholderDef(
            true,
            List.of(),
            "{x}, {z}"
        ));

        // exact-xyz – always show exact X, Y, Z (for admins / debug)
        map.put("exact-xyz", new PlaceholderDef(
            true,
            List.of(),
            "{x}, {y}, {z}"
        ));

        // state – short egg state string, HIDDEN when visibility mode = HIDDEN
        map.put("state", new PlaceholderDef(
            false,
            List.of(),
            "{state}"
        ));

        // bearer_name – name of current bearer (Hidden when visibility is HIDDEN)
        map.put("bearer_name", new PlaceholderDef(
            false,
            List.of(
                new ConditionEntry("{state} == 'HIDDEN'", "Unknown"),
                new ConditionEntry("{state} == 'PLAYER'", "{bearer}")
            ),
            "None"
        ));

        // dimension – dimension name (Hidden when visibility is HIDDEN)
        map.put("dimension", new PlaceholderDef(
            false,
            List.of(
                new ConditionEntry("{state} == 'HIDDEN'", "Unknown")
            ),
            "{dimension}"
        ));

        // ability_status – combined ability status string
        map.put("ability_status", new PlaceholderDef(
            true,
            List.of(),
            "{state} | dur:{ability_duration} cd:{ability_cooldown}"
        ));

        // server_info – online/max players
        map.put("server_info", new PlaceholderDef(
            true,
            List.of(),
            "{online}/{max_players}"
        ));

        return map;
    }
}
