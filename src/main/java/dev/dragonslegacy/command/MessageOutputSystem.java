package dev.dragonslegacy.command;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.config.MessagesConfig;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified message output system for Dragon's Legacy commands.
 *
 * <p>Supports single-player dispatch (backward compatible) and multi-channel broadcast
 * with visibility routing (bearer_only, everyone, everyone_except_bearer, executor_only,
 * everyone_except_executor).
 */
public final class MessageOutputSystem {

    private static final Set<String> VALID_MODES =
        Set.of("chat", "actionbar", "bossbar", "title", "subtitle");

    private static final String DEFAULT_OUTPUT_MODE = "chat";

    /** Bossbar duration in server ticks (5 seconds). */
    private static final int BOSSBAR_DURATION_TICKS = 100;

    private static final ConcurrentHashMap<UUID, ServerBossEvent> ACTIVE_BOSS_BARS =
        new ConcurrentHashMap<>();

    private MessageOutputSystem() {}

    // =========================================================================
    // Single-player dispatch (backward compatible)
    // =========================================================================

    /**
     * Sends {@code entry}'s primary (first) channel to {@code player}.
     * Silently ignores disabled entries.
     *
     * @param player the recipient
     * @param entry  the message entry to send; silently ignored if {@code null} or disabled
     */
    public static void send(ServerPlayer player, MessagesConfig.MessageEntry entry) {
        if (player == null || entry == null || entry.disabled) return;

        String mode = entry.output != null ? entry.output.toLowerCase(Locale.ROOT).trim() : DEFAULT_OUTPUT_MODE;
        if (!VALID_MODES.contains(mode)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Unknown output mode '{}' in messages config; falling back to '{}'.",
                entry.output, DEFAULT_OUTPUT_MODE);
            mode = DEFAULT_OUTPUT_MODE;
        }

        Component component = resolveComponent(player, entry.text);
        dispatch(player, mode, component);
    }

    // =========================================================================
    // Multi-channel broadcast with visibility routing
    // =========================================================================

    /**
     * Broadcasts a multi-channel message to all online players, applying visibility rules.
     *
     * <p>Visibility modes:
     * <ul>
     *   <li>{@code everyone}                  - sent to all online players</li>
     *   <li>{@code bearer_only}               - sent only to the current bearer</li>
     *   <li>{@code everyone_except_bearer}    - sent to all except the bearer</li>
     *   <li>{@code executor_only}             - sent only to the executor</li>
     *   <li>{@code everyone_except_executor}  - sent to all except the executor</li>
     * </ul>
     *
     * @param server   the Minecraft server
     * @param entry    the message entry to broadcast
     * @param executor the player who triggered the action (may be {@code null})
     * @param bearer   the current egg bearer (may be {@code null})
     */
    public static void broadcast(MinecraftServer server, MessagesConfig.MessageEntry entry,
                                  @Nullable ServerPlayer executor, @Nullable ServerPlayer bearer) {
        if (server == null || entry == null) return;

        List<MessagesConfig.ChannelEntry> channels = entry.channels;
        if (channels == null || channels.isEmpty()) {
            if (executor != null) send(executor, entry);
            return;
        }

        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();

        for (MessagesConfig.ChannelEntry channel : channels) {
            String mode = normalizeMode(channel.mode);
            String visibility = channel.getAudience();

            for (ServerPlayer player : allPlayers) {
                if (!matchesVisibility(visibility, player, executor, bearer)) continue;

                Component component = resolveComponent(player, channel.getResolvedText());
                dispatch(player, mode, component);
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static boolean matchesVisibility(String visibility, ServerPlayer player,
                                              @Nullable ServerPlayer executor,
                                              @Nullable ServerPlayer bearer) {
        return switch (visibility) {
            case "bearer_only" -> bearer != null && player.getUUID().equals(bearer.getUUID());
            case "everyone_except_bearer" -> bearer == null || !player.getUUID().equals(bearer.getUUID());
            case "executor_only" -> executor != null && player.getUUID().equals(executor.getUUID());
            case "everyone_except_executor" -> executor == null || !player.getUUID().equals(executor.getUUID());
            default -> true; // "everyone"
        };
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return DEFAULT_OUTPUT_MODE;
        String normalized = mode.toLowerCase(Locale.ROOT).trim();
        if (!VALID_MODES.contains(normalized)) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Unknown output mode '{}'; falling back to 'chat'.", mode);
            return DEFAULT_OUTPUT_MODE;
        }
        return normalized;
    }

    private static Component resolveComponent(ServerPlayer player, dev.dragonslegacy.config.MessageString ms) {
        String rawText = (ms != null) ? ms.value : "";
        try {
            if (ms != null && ms.node != null) {
                return ms.node.toText(PlaceholderContext.of(player));
            }
        } catch (Exception e) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Failed to resolve message text '{}': {}", rawText, e.getMessage());
        }
        return Component.literal(rawText);
    }

    private static void dispatch(ServerPlayer player, String mode, Component component) {
        switch (mode) {
            case "actionbar" -> sendActionBar(player, component);
            case "bossbar"   -> sendBossBar(player, component);
            case "title"     -> sendTitle(player, component);
            case "subtitle"  -> sendSubtitle(player, component);
            default          -> player.sendSystemMessage(component);
        }
    }

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

    private static void sendBossBar(ServerPlayer player, Component component) {
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

    // =========================================================================
    // Validation
    // =========================================================================

    /**
     * Validates all entries in the given {@link MessagesConfig}.
     * Logs a warning per invalid entry; never throws.
     */
    public static void validateAll(MessagesConfig cfg) {
        if (cfg == null) return;
        String[] keys = {
            "help", "bearer_info", "bearer_none",
            "ability_activated", "ability_deactivated", "ability_expired",
            "ability_cooldown_started", "ability_cooldown_ended",
            "not_bearer", "elytra_blocked",
            "egg_picked_up", "egg_dropped", "egg_placed", "egg_teleported",
            "bearer_changed", "bearer_cleared"
        };
        int warnings = 0;
        for (String key : keys) {
            MessagesConfig.MessageEntry entry = cfg.getEntry(key);
            if (entry.text == null) {
                DragonsLegacyMod.LOGGER.warn(
                    "[Dragon's Legacy] messages.yaml: entry '{}' has null text.", key);
                warnings++;
            }
        }
        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Loaded {} messages ({} warnings).", keys.length, warnings);
    }
}
