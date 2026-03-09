package dev.dragonslegacy.command;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.config.MessagesConfig;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified message output system for Dragon's Legacy commands.
 *
 * <p>Parses message text using the PB4 NodeParser (which supports MiniMessage and
 * Placeholder API) and dispatches the resulting {@link Component} to the target player
 * via the configured output mode.
 *
 * <h3>Supported output modes</h3>
 * <ul>
 *   <li>{@code chat}      – standard chat message via {@link ServerPlayer#sendSystemMessage}</li>
 *   <li>{@code actionbar} – above-hotbar message via {@link ClientboundSetActionBarTextPacket}</li>
 *   <li>{@code bossbar}   – temporary boss bar (auto-removed after 5 seconds)</li>
 *   <li>{@code title}     – large on-screen title via {@link ClientboundSetTitleTextPacket}</li>
 *   <li>{@code subtitle}  – small on-screen subtitle via {@link ClientboundSetSubtitleTextPacket}</li>
 * </ul>
 *
 * <h3>Validation</h3>
 * <ul>
 *   <li>Unknown output modes fall back to {@code chat} with a warning log.</li>
 *   <li>If MiniMessage / PB4 parsing fails the raw text is used instead.</li>
 *   <li>A {@code null} entry or null player is silently ignored.</li>
 * </ul>
 */
public final class MessageOutputSystem {

    private static final Set<String> VALID_MODES =
        Set.of("chat", "actionbar", "bossbar", "title", "subtitle");

    /** Default output mode used when the configured mode is absent or invalid. */
    private static final String DEFAULT_OUTPUT_MODE = "chat";

    /** Bossbar duration in server ticks (5 seconds). */
    private static final int BOSSBAR_DURATION_TICKS = 100;

    private static final ConcurrentHashMap<UUID, ServerBossEvent> ACTIVE_BOSS_BARS =
        new ConcurrentHashMap<>();

    private MessageOutputSystem() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sends {@code entry} to {@code player} using the entry's configured output mode.
     *
     * <p>PB4 global placeholders and MiniMessage tags are resolved relative to
     * {@code player}'s context so that player-specific placeholders work correctly.
     *
     * @param player the recipient
     * @param entry  the message entry to send; silently ignored if {@code null}
     */
    public static void send(ServerPlayer player, MessagesConfig.MessageEntry entry) {
        if (player == null || entry == null) return;

        // Validate and normalise output mode
        String mode = entry.output != null ? entry.output.toLowerCase(java.util.Locale.ROOT).trim() : DEFAULT_OUTPUT_MODE;
        if (!VALID_MODES.contains(mode)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Unknown output mode '{}' in messages config; falling back to '{}'.",
                entry.output, DEFAULT_OUTPUT_MODE
            );
            mode = DEFAULT_OUTPUT_MODE;
        }

        // Resolve text → MC Component
        Component component = resolveComponent(player, entry);

        // Dispatch
        switch (mode) {
            case "actionbar" -> sendActionBar(player, component);
            case "bossbar"   -> sendBossBar(player, component);
            case "title"     -> sendTitle(player, component);
            case "subtitle"  -> sendSubtitle(player, component);
            default          -> player.sendSystemMessage(component); // "chat"
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers – component resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves the entry's text into a Minecraft {@link Component}.
     *
     * <p>Uses the pre-built PB4 {@link eu.pb4.placeholders.api.node.TextNode} stored in
     * the {@link dev.dragonslegacy.config.MessageString} to expand PB4 global placeholders
     * (including {@code %deg:bearer%}, etc.) in the context of {@code player}.
     *
     * <p>Falls back to a plain literal if the entry has no node or if expansion throws.
     */
    private static Component resolveComponent(ServerPlayer player, MessagesConfig.MessageEntry entry) {
        String rawText = (entry.text != null) ? entry.text.value : "";
        try {
            if (entry.text != null && entry.text.node != null) {
                return entry.text.node.toText(PlaceholderContext.of(player));
            }
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Failed to resolve message text '{}': {}",
                rawText, e.getMessage()
            );
        }
        return Component.literal(rawText);
    }

    // -------------------------------------------------------------------------
    // Private helpers – output modes
    // -------------------------------------------------------------------------

    private static void sendActionBar(ServerPlayer player, Component component) {
        player.connection.send(new ClientboundSetActionBarTextPacket(component));
    }

    private static void sendTitle(ServerPlayer player, Component component) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(component));
    }

    private static void sendSubtitle(ServerPlayer player, Component component) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(component));
    }

    /**
     * Shows a temporary boss bar to {@code player} that disappears after
     * {@value #BOSSBAR_DURATION_TICKS} ticks.  Any previously active boss bar
     * for the same player is removed first to avoid accumulation.
     */
    private static void sendBossBar(ServerPlayer player, Component component) {
        // Remove any existing boss bar for this player first
        ServerBossEvent existing = ACTIVE_BOSS_BARS.remove(player.getUUID());
        if (existing != null) {
            existing.removePlayer(player);
        }

        ServerBossEvent bossEvent = new ServerBossEvent(
            component,
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
        );
        bossEvent.setProgress(1.0f);
        bossEvent.addPlayer(player);
        ACTIVE_BOSS_BARS.put(player.getUUID(), bossEvent);

        // Schedule removal
        net.minecraft.server.MinecraftServer srv = DragonsLegacyMod.server;
        if (srv != null) {
            int removeAt = srv.getTickCount() + BOSSBAR_DURATION_TICKS;
            srv.schedule(new TickTask(removeAt, () -> {
                ServerBossEvent toRemove = ACTIVE_BOSS_BARS.remove(player.getUUID());
                if (toRemove != null) {
                    toRemove.removePlayer(player);
                }
            }));
        }
    }

    // -------------------------------------------------------------------------
    // Validation helper (called at config load)
    // -------------------------------------------------------------------------

    /**
     * Validates the output mode of a single {@link MessagesConfig.MessageEntry}.
     * Logs a warning if the mode is invalid; never throws.
     *
     * @param key   the config key name (used in the log message)
     * @param entry the entry to validate
     */
    public static void validateEntry(String key, MessagesConfig.MessageEntry entry) {
        if (entry == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: entry '{}' is null – using defaults.", key);
            return;
        }
        if (entry.output == null || !VALID_MODES.contains(entry.output.toLowerCase(java.util.Locale.ROOT).trim())) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: entry '{}' has invalid output mode '{}'. "
                + "Allowed: {}. Falling back to '{}'.",
                key, entry.output, VALID_MODES, DEFAULT_OUTPUT_MODE
            );
        }
        if (entry.text == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] messages.yaml: entry '{}' has a null text field.", key);
        }
    }

    /**
     * Validates all entries in the given {@link MessagesConfig}.
     * Logs a warning per invalid entry; never throws.
     *
     * @param cfg the config to validate; silently ignored if {@code null}
     */
    public static void validateAll(MessagesConfig cfg) {
        if (cfg == null) return;
        validateEntry("help",             cfg.help);
        validateEntry("bearer_info",      cfg.bearerInfo);
        validateEntry("bearer_none",      cfg.bearerNone);
        validateEntry("hunger_activate",  cfg.hungerActivate);
        validateEntry("hunger_deactivate", cfg.hungerDeactivate);
        validateEntry("not_bearer",       cfg.notBearer);
    }
}
