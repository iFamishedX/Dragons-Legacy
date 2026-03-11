package dev.dragonslegacy;

import dev.dragonslegacy.ability.AbilityTimers;
import dev.dragonslegacy.config.EggConfig;
import dev.dragonslegacy.config.PlaceholdersConfig;
import dev.dragonslegacy.config.VisibilityType;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggState;
import dev.dragonslegacy.egg.EggTracker;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.api.DragonEggAPI.PositionType;
import dev.dragonslegacy.config.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves config-driven placeholders defined in {@code placeholders.yaml}.
 *
 * <h3>Resolution algorithm</h3>
 * <ol>
 *   <li>Build an internal variable map from current game state ({@link #buildInternalVars}).</li>
 *   <li>Evaluate {@link PlaceholdersConfig.PlaceholderDef#conditions conditions} top-to-bottom;
 *       if a condition matches, process its {@code output} template and return.</li>
 *   <li>If no condition matches, process {@link PlaceholdersConfig.PlaceholderDef#format format}
 *       as the fallback template.</li>
 * </ol>
 *
 * <h3>Template processing</h3>
 * Templates use {@code {expression}} tokens evaluated innermost-first.  Expressions can be:
 * <ul>
 *   <li>An internal placeholder name: {@code {x}}, {@code {bearer}}, {@code {state}}, …</li>
 *   <li>A filter call: {@code {upper(value)}}, {@code {round({x},50)}}, …</li>
 * </ul>
 *
 * <h3>Supported filters</h3>
 * {@code upper}, {@code lower}, {@code capitalize}, {@code round}, {@code abs},
 * {@code format_number}, {@code default}, {@code replace}, {@code if},
 * {@code color}, {@code json}, {@code distance_to_player}.
 *
 * <h3>Visibility rules</h3>
 * When {@link PlaceholdersConfig.PlaceholderDef#ignoreVisibility ignoreVisibility} is
 * {@code false}, the synthesised {@code {state}} internal placeholder returns
 * {@code "HIDDEN"} if the current position type's visibility mode is
 * {@link VisibilityType#HIDDEN}.  This lets configs handle the hidden case via conditions.
 * When {@code ignoreVisibility} is {@code true}, the real egg state is always exposed.
 */
public class PlaceholderEngine {

    // -------------------------------------------------------------------------
    // Internal placeholder names (not exposed to PlaceholderAPI)
    // -------------------------------------------------------------------------

    private static final Pattern INNERMOST_TOKEN =
        Pattern.compile("\\{([^{}]+)}");

    private static final Pattern FUNC_CALL =
        Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\((.*)\\)$", Pattern.DOTALL);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolves a single placeholder definition against the current game state.
     *
     * @param def    the placeholder definition from {@code placeholders.yaml}
     * @param player the player requesting the placeholder (may be {@code null} for server sources)
     * @return the resolved string
     */
    public static String resolve(PlaceholdersConfig.PlaceholderDef def, ServerPlayer player) {
        Map<String, String> vars = buildInternalVars(def.ignoreVisibility, player);

        // Evaluate conditions top-to-bottom
        for (PlaceholdersConfig.ConditionEntry cond : def.conditions) {
            if (cond.condition != null && !cond.condition.isBlank()) {
                String resolvedCondition = substituteVars(cond.condition, vars);
                if (evaluateCondition(resolvedCondition)) {
                    return processTemplate(cond.output != null ? cond.output : "", vars, player);
                }
            }
        }

        // Fallback: process format
        return processTemplate(def.format != null ? def.format : "", vars, player);
    }

    // -------------------------------------------------------------------------
    // Internal variable map
    // -------------------------------------------------------------------------

    /**
     * Builds the map of all internal placeholder names → their current values.
     *
     * @param ignoreVisibility when {@code true}, exact egg coordinates are always included
     *                         and {@code {state}} is never synthesised to "HIDDEN"
     * @param requestingPlayer the player who requested the placeholder (may be null)
     */
    private static Map<String, String> buildInternalVars(boolean ignoreVisibility, ServerPlayer requestingPlayer) {
        Map<String, String> vars = new LinkedHashMap<>();

        MinecraftServer server = DragonsLegacyMod.server;
        DragonsLegacy legacy   = DragonsLegacy.getInstance();
        Data data              = DragonEggAPI.getData();
        EggTracker tracker     = (legacy != null) ? legacy.getEggTracker() : null;

        // ---------- Egg coordinates ----------
        int rawX = 0, rawY = 0, rawZ = 0;
        if (data != null) {
            BlockPos pos = data.getBlockPos();
            if (pos != null) {
                rawX = pos.getX();
                rawY = pos.getY();
                rawZ = pos.getZ();
            }
        }
        vars.put("x", String.valueOf(rawX));
        vars.put("y", String.valueOf(rawY));
        vars.put("z", String.valueOf(rawZ));

        // ---------- Dimension ----------
        String dimension = (data != null && data.worldId != null) ? data.worldId : "unknown";
        vars.put("dimension", dimension);

        // ---------- State (with visibility synthesis) ----------
        EggState eggState = (tracker != null) ? tracker.getCurrentState() : EggState.UNKNOWN;
        PositionType posType = (data != null && data.type != null) ? data.type : PositionType.BLOCK;

        String stateName;
        if (!ignoreVisibility) {
            VisibilityType visibility = resolveVisibility(posType);
            if (visibility == VisibilityType.HIDDEN) {
                stateName = "HIDDEN";
            } else {
                stateName = eggStateToStateName(eggState);
            }
        } else {
            stateName = eggStateToStateName(eggState);
        }
        vars.put("state", stateName);

        // ---------- Bearer ----------
        String bearerName = "none";
        String bearerUuidStr = "";
        if (tracker != null && tracker.getCurrentBearer() != null) {
            UUID bearerUUID = tracker.getCurrentBearer();
            bearerUuidStr = bearerUUID.toString();
            if (server != null) {
                ServerPlayer bearerPlayer = server.getPlayerList().getPlayer(bearerUUID);
                bearerName = bearerPlayer != null ? bearerPlayer.getGameProfile().name() : bearerUUID.toString();
            } else {
                bearerName = bearerUUID.toString();
            }
        }
        vars.put("bearer", bearerName);
        vars.put("bearer_uuid", bearerUuidStr);

        // ---------- Executor (requesting player) ----------
        if (requestingPlayer != null) {
            vars.put("executor", requestingPlayer.getGameProfile().name());
            vars.put("executor_uuid", requestingPlayer.getUUID().toString());
        } else {
            vars.put("executor", "server");
            vars.put("executor_uuid", "");
        }

        // ---------- Last seen / seconds ----------
        // Stub: will return a real value once last-seen tracking is implemented.
        vars.put("last_seen", "0");
        vars.put("seconds", "0");

        // ---------- Egg age ----------
        vars.put("egg_age", "0");

        // ---------- Ability timers ----------
        int abilityDuration = 0, abilityCooldown = 0;
        if (legacy != null) {
            AbilityTimers timers = legacy.getAbilityEngine().getTimers();
            abilityDuration = timers.getDurationRemaining();
            abilityCooldown = timers.getCooldownRemaining();
        }
        vars.put("ability_duration", String.valueOf(abilityDuration));
        vars.put("ability_cooldown", String.valueOf(abilityCooldown));

        // ---------- Server counts ----------
        int online    = server != null ? server.getPlayerList().getPlayerCount() : 0;
        int maxPlayers = server != null ? server.getPlayerList().getMaxPlayers() : 0;
        vars.put("online", String.valueOf(online));
        vars.put("max_players", String.valueOf(maxPlayers));

        // ---------- Time ----------
        long worldTimeTick = 0;
        if (server != null && server.overworld() != null) {
            worldTimeTick = server.overworld().getDayTime();
        }
        vars.put("world_time", String.valueOf(worldTimeTick));
        vars.put("tick", String.valueOf(worldTimeTick));
        vars.put("real_time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // ---------- Bearer health ----------
        float bearerHealth = 0f, bearerMaxHealth = 0f;
        if (tracker != null && tracker.getCurrentBearer() != null && server != null) {
            ServerPlayer bp = server.getPlayerList().getPlayer(tracker.getCurrentBearer());
            if (bp != null) {
                bearerHealth    = bp.getHealth();
                bearerMaxHealth = bp.getMaxHealth();
            }
        }
        vars.put("bearer_health",     formatFloat(bearerHealth));
        vars.put("bearer_max_health", formatFloat(bearerMaxHealth));

        return vars;
    }

    // -------------------------------------------------------------------------
    // Template processing
    // -------------------------------------------------------------------------

    /**
     * Replaces all {@code {expression}} tokens in a template string with their resolved values.
     * Evaluates innermost tokens first (handles nesting like {@code {round({x},50)}}).
     */
    private static String processTemplate(String template, Map<String, String> vars, ServerPlayer player) {
        // Iteratively replace innermost {…} tokens until no more remain
        String result = template;
        int maxIterations = 64; // guard against runaway loops
        for (int i = 0; i < maxIterations; i++) {
            Matcher m = INNERMOST_TOKEN.matcher(result);
            if (!m.find()) break;

            StringBuffer sb = new StringBuffer();
            m.reset();
            boolean anyMatch = false;
            while (m.find()) {
                anyMatch = true;
                String expr   = m.group(1);
                String value  = evaluateExpression(expr, vars, player);
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            m.appendTail(sb);
            if (!anyMatch) break;
            result = sb.toString();
        }
        return result;
    }

    /**
     * Performs a simple variable substitution (replaces {name} tokens with values)
     * without filter evaluation – used for condition strings before boolean evaluation.
     */
    private static String substituteVars(String template, Map<String, String> vars) {
        Matcher m = INNERMOST_TOKEN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key   = m.group(1).trim();
            String value = vars.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Expression evaluation
    // -------------------------------------------------------------------------

    /**
     * Evaluates a single expression (already stripped of surrounding {@code { }}).
     * The expression can be a plain variable name, a literal, or a filter call.
     */
    private static String evaluateExpression(String expr, Map<String, String> vars, ServerPlayer player) {
        expr = expr.trim();

        // Plain variable lookup (no parentheses)
        if (vars.containsKey(expr)) {
            return vars.get(expr);
        }

        // Check for a function call pattern: name(args...)
        Matcher funcMatcher = FUNC_CALL.matcher(expr);
        if (funcMatcher.matches()) {
            String funcName = funcMatcher.group(1).toLowerCase(Locale.ROOT);
            String argsStr  = funcMatcher.group(2);
            List<String> args = splitArgs(argsStr);
            return applyFilter(funcName, args, vars, player);
        }

        // Not a known variable or function → return as-is (might be a literal)
        return expr;
    }

    // -------------------------------------------------------------------------
    // Filter implementations
    // -------------------------------------------------------------------------

    /**
     * Applies the named filter to the given (already-resolved) arguments.
     */
    private static String applyFilter(String name, List<String> rawArgs,
                                      Map<String, String> vars, ServerPlayer player) {
        // Resolve any {…} tokens inside the raw argument strings
        List<String> args = new ArrayList<>(rawArgs.size());
        for (String a : rawArgs) {
            args.add(processTemplate(a.trim(), vars, player));
        }

        return switch (name) {
            case "upper" -> args.isEmpty() ? "" : args.get(0).toUpperCase(Locale.ROOT);
            case "lower" -> args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
            case "capitalize" -> {
                if (args.isEmpty()) yield "";
                String v = args.get(0);
                yield v.isEmpty() ? v : Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase(Locale.ROOT);
            }
            case "round" -> {
                if (args.size() < 2) yield args.isEmpty() ? "0" : args.get(0);
                try {
                    double value     = Double.parseDouble(args.get(0));
                    double precision = Double.parseDouble(args.get(1));
                    if (precision <= 0) yield String.valueOf((long) Math.round(value));
                    long rounded = Math.round(value / precision) * (long) precision;
                    yield String.valueOf(rounded);
                } catch (NumberFormatException e) {
                    yield args.get(0);
                }
            }
            case "abs" -> {
                if (args.isEmpty()) yield "0";
                try {
                    yield String.valueOf(Math.abs(Double.parseDouble(args.get(0))));
                } catch (NumberFormatException e) {
                    yield args.get(0);
                }
            }
            case "format_number" -> {
                if (args.isEmpty()) yield "0";
                try {
                    double v = Double.parseDouble(args.get(0));
                    DecimalFormat df = new DecimalFormat("#,##0.##",
                        DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    yield df.format(v);
                } catch (NumberFormatException e) {
                    yield args.get(0);
                }
            }
            case "default" -> {
                if (args.isEmpty()) yield "";
                String val      = args.get(0);
                String fallback = args.size() > 1 ? args.get(1) : "";
                yield (val == null || val.isBlank() || val.equals("0")) ? fallback : val;
            }
            case "replace" -> {
                if (args.size() < 3) yield args.isEmpty() ? "" : args.get(0);
                yield args.get(0).replace(args.get(1), args.get(2));
            }
            case "if" -> {
                // if(condition, trueValue, falseValue)
                if (args.size() < 3) yield args.isEmpty() ? "" : args.get(0);
                String condStr   = substituteVars(args.get(0), vars);
                boolean condTrue = evaluateCondition(condStr);
                yield condTrue ? args.get(1) : args.get(2);
            }
            case "color" -> {
                // Convert legacy §-codes to MiniMessage equivalent, or pass through
                if (args.isEmpty()) yield "";
                yield args.get(0).replace("§", "&");
            }
            case "json" -> {
                // Produce a JSON array string from all args
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(args.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
                }
                sb.append("]");
                yield sb.toString();
            }
            case "distance_to_player" -> {
                // Distance from the egg to the requesting player
                if (player == null) yield "?";
                Data data = DragonEggAPI.getData();
                if (data == null) yield "?";
                BlockPos eggPos = data.getBlockPos();
                if (eggPos == null) yield "?";
                double dx = player.getX() - eggPos.getX();
                double dy = player.getY() - eggPos.getY();
                double dz = player.getZ() - eggPos.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                yield String.valueOf((long) Math.round(dist));
            }
            default -> {
                // Unknown filter – return the first arg unchanged, if available
                yield args.isEmpty() ? "" : args.get(0);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Condition evaluation
    // -------------------------------------------------------------------------

    /**
     * Evaluates a simple boolean condition string.
     * Supported operators: {@code ==}, {@code !=}, {@code >=}, {@code <=}, {@code >}, {@code <}.
     * Values may be quoted strings ({@code 'value'}) or numeric literals.
     * After variable substitution, the remaining text is compared as strings or numbers.
     */
    /** Evaluates a simple boolean condition string (after variable substitution). */
    private static boolean evaluateCondition(String condition) {
        condition = condition.trim();

        // Try each operator (longer first to avoid ambiguity)
        for (String op : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
            int idx = findOperatorIndex(condition, op);
            if (idx < 0) continue;

            String left  = unquote(condition.substring(0, idx).trim());
            String right = unquote(condition.substring(idx + op.length()).trim());

            return compare(left, right, op);
        }

        // No operator → treat as truthy check (non-empty and non-"false")
        return !condition.isBlank()
            && !condition.equalsIgnoreCase("false")
            && !condition.equals("0");
    }

    /**
     * Finds the first occurrence of {@code op} in {@code s} that is NOT inside quotes.
     * Returns -1 if not found.
     */
    private static int findOperatorIndex(String s, String op) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) { inSingle = !inSingle; continue; }
            if (c == '"'  && !inSingle) { inDouble = !inDouble; continue; }
            if (!inSingle && !inDouble && s.startsWith(op, i)) return i;
        }
        return -1;
    }

    private static boolean compare(String left, String right, String op) {
        // Try numeric comparison first
        try {
            double l = Double.parseDouble(left);
            double r = Double.parseDouble(right);
            double epsilon = 1e-9;
            return switch (op) {
                case "==" -> Math.abs(l - r) < epsilon;
                case "!=" -> Math.abs(l - r) >= epsilon;
                case ">=" -> l >= r - epsilon;
                case "<=" -> l <= r + epsilon;
                case ">"  -> l >  r + epsilon;
                case "<"  -> l <  r - epsilon;
                default   -> false;
            };
        } catch (NumberFormatException ignored) {
            // Fall through to string comparison
        }
        int cmp = left.compareToIgnoreCase(right);
        return switch (op) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">"  -> cmp >  0;
            case "<"  -> cmp <  0;
            default   -> false;
        };
    }

    /** Strips surrounding single or double quotes from a string. */
    private static String unquote(String s) {
        if (s.length() >= 2
                && ((s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'')
                ||  (s.charAt(0) == '"'  && s.charAt(s.length() - 1) == '"'))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    // -------------------------------------------------------------------------
    // Argument splitting
    // -------------------------------------------------------------------------

    /**
     * Splits a comma-separated argument string into individual args,
     * respecting balanced parentheses (so nested function calls are not split mid-call).
     */
    private static List<String> splitArgs(String argsStr) {
        List<String> result = new ArrayList<>();
        if (argsStr == null || argsStr.isBlank()) return result;

        int depth = 0;
        int start = 0;
        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                result.add(argsStr.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(argsStr.substring(start).trim());
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Maps an {@link EggState} to the short state name used in placeholder conditions. */
    private static String eggStateToStateName(EggState state) {
        return switch (state) {
            case PLAYER  -> "PLAYER";
            case BLOCK   -> "BLOCK";
            // NOTE: previously returned "DROPPED" (from DROPPED_ITEM enum value).
            // Changed to "WORLD" to match the renamed EggState.WORLD enum value.
            // Update any placeholder conditions that previously checked for "DROPPED".
            case WORLD   -> "WORLD";
            case UNKNOWN -> "UNKNOWN";
        };
    }

    /** Returns the current {@link VisibilityType} for a given {@link PositionType}. */
    private static VisibilityType resolveVisibility(PositionType posType) {
        EggConfig cfg = DragonsLegacyMod.configManager.getEggConfig();
        if (cfg == null || cfg.visibility == null) return VisibilityType.EXACT;
        VisibilityType vt = cfg.visibility.get(posType);
        return vt != null ? vt : VisibilityType.EXACT;
    }

    private static String formatFloat(float value) {
        // Avoid unnecessary .0 for whole numbers
        if (value == Math.floor(value) && !Float.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
