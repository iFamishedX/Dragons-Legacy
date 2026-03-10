package dev.dragonslegacy;

import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.ability.AbilityTimers;
import dev.dragonslegacy.api.APIUtils;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.config.Data;
import dev.dragonslegacy.config.PlaceholdersConfig;
import dev.dragonslegacy.config.VisibilityType;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggTracker;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Placeholders {

    // =========================================================================
    // %dragonslegacy:*% placeholders
    // =========================================================================

    private static final Map<Identifier, PlaceholderHandler> ALL_PLACEHOLDERS = new HashMap<>();

    static {
        // Player who triggered the action / executor
        ALL_PLACEHOLDERS.put(dlIdentifier("player"), (ctx, arg) -> {
            if (ctx.player() == null) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(ctx.player().getGameProfile().name());
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("executor"), (ctx, arg) -> {
            if (ctx.player() == null) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(ctx.player().getGameProfile().name());
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("executor_uuid"), (ctx, arg) -> {
            if (ctx.player() == null) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(ctx.player().getUUID().toString());
        });

        // Bearer
        ALL_PLACEHOLDERS.put(dlIdentifier("bearer"), (ctx, arg) ->
            PlaceholderResult.value(APIUtils.getBearer()));

        // Global prefix from MessagesConfig
        ALL_PLACEHOLDERS.put(dlIdentifier("global_prefix"), (ctx, arg) -> {
            String prefix = DragonsLegacyMod.configManager.getMessages().prefix;
            return PlaceholderResult.value(prefix != null ? prefix : "");
        });

        // Egg coordinates
        ALL_PLACEHOLDERS.put(dlIdentifier("x"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("?");
            return PlaceholderResult.value(String.valueOf(data.getBlockPos().getX()));
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("y"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("?");
            return PlaceholderResult.value(String.valueOf(data.getBlockPos().getY()));
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("z"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("?");
            return PlaceholderResult.value(String.valueOf(data.getBlockPos().getZ()));
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("dimension"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("unknown");
            String dim = data.worldId != null ? data.worldId : "unknown";
            return PlaceholderResult.value(dim);
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("egg_location"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("unknown");
            return PlaceholderResult.value(data.getBlockPos().toShortString());
        });

        // Egg state
        ALL_PLACEHOLDERS.put(dlIdentifier("egg_state"), (ctx, arg) -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return PlaceholderResult.value("unknown");
            return PlaceholderResult.value(
                legacy.getEggTracker().getCurrentState().name().toLowerCase(java.util.Locale.ROOT));
        });

        ALL_PLACEHOLDERS.put(dlIdentifier("last_seen"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.value("never");
            // Stub: returns "0" until last-seen tracking is implemented in a future update.
            return PlaceholderResult.value("0");
        });

        // Stub: alias for last_seen in seconds format – will return a real value once tracking is implemented.
        ALL_PLACEHOLDERS.put(dlIdentifier("seconds"), (ctx, arg) ->
            PlaceholderResult.value("0"));

        // Ability duration remaining (ticks)
        ALL_PLACEHOLDERS.put(dlIdentifier("ability_duration"), (ctx, arg) -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return PlaceholderResult.value("0");
            AbilityTimers timers = legacy.getAbilityEngine().getTimers();
            return PlaceholderResult.value(String.valueOf(timers.getDurationRemaining()));
        });

        // Ability cooldown remaining (ticks)
        ALL_PLACEHOLDERS.put(dlIdentifier("ability_cooldown"), (ctx, arg) -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return PlaceholderResult.value("0");
            AbilityTimers timers = legacy.getAbilityEngine().getTimers();
            return PlaceholderResult.value(String.valueOf(timers.getCooldownRemaining()));
        });

        // Online player count
        ALL_PLACEHOLDERS.put(dlIdentifier("online"), (ctx, arg) -> {
            MinecraftServer server = DragonsLegacyMod.server;
            if (server == null) return PlaceholderResult.value("0");
            return PlaceholderResult.value(String.valueOf(server.getPlayerList().getPlayerCount()));
        });

        // Max players
        ALL_PLACEHOLDERS.put(dlIdentifier("max_players"), (ctx, arg) -> {
            MinecraftServer server = DragonsLegacyMod.server;
            if (server == null) return PlaceholderResult.value("0");
            return PlaceholderResult.value(String.valueOf(server.getPlayerList().getMaxPlayers()));
        });

        // =====================================================================
        // Legacy "deg:" namespace (backward compat)
        // =====================================================================

        ALL_PLACEHOLDERS.put(degIdentifier("bearer"), (ctx, arg) ->
            PlaceholderResult.value(APIUtils.getBearer()));

        ALL_PLACEHOLDERS.put(degIdentifier("exact_pos"), (ctx, arg) -> {
            if (!Permissions.check(ctx.source(), Perms.EXACT_POS_PLACEHOLDER, PermissionLevel.ADMINS))
                return PlaceholderResult.invalid("No Permission");
            if (DragonEggAPI.getData() == null) return PlaceholderResult.invalid("No Data");
            return PlaceholderResult.value(DragonEggAPI.getData().getBlockPos().toShortString());
        });

        ALL_PLACEHOLDERS.put(degIdentifier("randomized_pos"), (ctx, arg) -> {
            if (!Permissions.check(ctx.source(), Perms.RANDOMIZED_POS_PLACEHOLDER, PermissionLevel.ADMINS))
                return PlaceholderResult.invalid("No Permission");
            if (DragonEggAPI.getData() == null) return PlaceholderResult.invalid("No Data");
            return PlaceholderResult.value(DragonEggAPI.getData().getRandomizedPosition().toShortString());
        });

        ALL_PLACEHOLDERS.put(degIdentifier("pos"), (ctx, arg) -> {
            Data data = DragonEggAPI.getData();
            if (data == null) return PlaceholderResult.invalid("No Data");
            VisibilityType visibilityType = DragonsLegacyMod.configManager.getMain().getVisibility(data.type);
            return PlaceholderResult.value(
                switch (visibilityType) {
                    case EXACT -> data.getBlockPos().toShortString();
                    case RANDOMIZED -> data.getRandomizedPosition().toShortString();
                    case HIDDEN -> "Unknown";
                }
            );
        });

        ALL_PLACEHOLDERS.put(degIdentifier("item"), (ctx, arg) -> {
            ItemStack stack = Items.DRAGON_EGG.getDefaultInstance();
            MutableComponent text = Component.empty()
                .append(stack.getHoverName())
                .withStyle(stack.getRarity().color())
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(stack)));
            return PlaceholderResult.value(text);
        });
    }

    public static final Map<Identifier, PlaceholderHandler> PLACEHOLDERS = ALL_PLACEHOLDERS;

    public static Identifier dlIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(DragonsLegacyMod.MOD_ID, path);
    }

    public static Identifier degIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(DragonsLegacyMod.MOD_ID_ALIAS, path);
    }

    /** @deprecated Use {@link #dlIdentifier(String)} instead. */
    @Deprecated
    public static Identifier modIdentifier(String path) {
        return degIdentifier(path);
    }

    public static void register() {
        PLACEHOLDERS.forEach(eu.pb4.placeholders.api.Placeholders::register);
    }

    /**
     * Registers config-driven external placeholders from {@code placeholders.yaml}.
     *
     * <p>Each entry in {@link PlaceholdersConfig#placeholders} becomes one
     * {@code %dragonslegacy:name%} placeholder.  The value is resolved at call
     * time via {@link PlaceholderEngine#resolve(PlaceholdersConfig.PlaceholderDef, ServerPlayer)}.
     *
     * <p>This method must be called <em>after</em> {@link DragonsLegacyMod#configManager}
     * has been initialised (i.e. after {@code configManager.init()}).
     *
     * <p>Safe to call on reload: existing handlers are silently overwritten because
     * the PAPI registration is idempotent for the same {@link Identifier}.
     */
    public static void registerDynamic() {
        PlaceholdersConfig cfg = DragonsLegacyMod.configManager.getPlaceholders();
        if (cfg == null || cfg.placeholders == null || cfg.placeholders.isEmpty()) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] placeholders.yaml contains no placeholder definitions.");
            return;
        }

        int count = 0;
        for (Map.Entry<String, PlaceholdersConfig.PlaceholderDef> entry : cfg.placeholders.entrySet()) {
            String name = entry.getKey();
            PlaceholdersConfig.PlaceholderDef def = entry.getValue();
            if (def == null) continue;

            Identifier id = dlIdentifier(name);
            eu.pb4.placeholders.api.Placeholders.register(id, (ctx, arg) -> {
                // Re-read the definition at render time so hot-reloads are reflected
                PlaceholdersConfig liveCfg = DragonsLegacyMod.configManager.getPlaceholders();
                PlaceholdersConfig.PlaceholderDef liveDef =
                    (liveCfg != null && liveCfg.placeholders != null)
                        ? liveCfg.placeholders.get(name)
                        : null;
                if (liveDef == null) liveDef = def; // fallback to snapshot at registration time

                ServerPlayer player = ctx.player() != null ? ctx.player() : null;
                String value = PlaceholderEngine.resolve(liveDef, player);
                return PlaceholderResult.value(value);
            });
            count++;
        }

        DragonsLegacyMod.LOGGER.info(
            "[Dragon's Legacy] Registered {} dynamic placeholder(s) from placeholders.yaml.", count);
    }
}
