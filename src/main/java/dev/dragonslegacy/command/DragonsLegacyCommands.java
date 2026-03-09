package dev.dragonslegacy.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.Perms;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.ability.AbilityState;
import dev.dragonslegacy.ability.AbilityTimers;
import dev.dragonslegacy.config.MessagesConfig;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggState;
import dev.dragonslegacy.egg.EggTracker;
import dev.dragonslegacy.utils.Utils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Registers and handles the new {@code /dragonslegacy} (alias {@code /dl}) command tree.
 *
 * <h3>Public subcommands</h3>
 * <ul>
 *   <li>{@code /dragonslegacy help}       – Display configurable help message.</li>
 *   <li>{@code /dragonslegacy bearer}     – Show the current egg bearer.</li>
 *   <li>{@code /dragonslegacy hunger on}  – Activate Dragon's Hunger (bearer only).</li>
 *   <li>{@code /dragonslegacy hunger off} – Deactivate Dragon's Hunger (bearer only).</li>
 *   <li>{@code /dragonslegacy placeholders} – List all placeholder values.</li>
 * </ul>
 *
 * <h3>Admin subcommands (requires operator permission)</h3>
 * <ul>
 *   <li>{@code /dragonslegacy info}               – Full egg/ability status.</li>
 *   <li>{@code /dragonslegacy setbearer <player>} – Force-assign the bearer.</li>
 *   <li>{@code /dragonslegacy clearability}       – Deactivate the ability.</li>
 *   <li>{@code /dragonslegacy resetcooldown}      – Reset the cooldown.</li>
 *   <li>{@code /dragonslegacy reload}             – Reload all configs (op required).</li>
 * </ul>
 *
 * <p>All user-facing output is read from {@code config/dragonslegacy/messages.yaml}
 * via {@link MessagesConfig} and rendered by {@link MessageOutputSystem}.
 * Command names, aliases, and permission settings are loaded from
 * {@code config/dragonslegacy/global.yaml}.
 */
public class DragonsLegacyCommands {

    private DragonsLegacyCommands() {}

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers the {@code /dragonslegacy} command tree (and its {@code /dl} alias)
     * with the Fabric command dispatcher.  Must be called during mod initialisation.
     *
     * <p>Command names, aliases, and per-command permission settings are read from
     * {@link dev.dragonslegacy.config.GlobalConfig} via {@code global.yaml}.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dev.dragonslegacy.config.GlobalConfig global = DragonsLegacyMod.configManager.getGlobal();
            dev.dragonslegacy.config.GlobalConfig.CommandsSection cmds =
                global.commands != null ? global.commands : new dev.dragonslegacy.config.GlobalConfig.CommandsSection();

            // Resolve root name and subcommand names (fall back to defaults if blank)
            String rootName         = nonEmpty(cmds.root, "dragonslegacy");
            String helpName         = "help";
            String bearerName       = "bearer";
            String hungerName       = "hunger";
            String onName           = "on";
            String offName          = "off";
            String reloadName       = "reload";
            String placeholdersName = "placeholders";

            // Validate messages.yaml on registration so misconfigurations are logged early
            MessageOutputSystem.validateAll(DragonsLegacyMod.configManager.getMessages());

            // Build and register the root command
            dispatcher.register(
                literal(rootName)
                    // ── Public subcommands ──────────────────────────────────
                    .then(literal(helpName)
                        .requires(requirePerm(global, cmds.help))
                        .executes(DragonsLegacyCommands::help)
                    )
                    .then(literal(bearerName)
                        .requires(requirePerm(global, cmds.bearer))
                        .executes(DragonsLegacyCommands::bearer)
                    )
                    .then(literal(hungerName)
                        .requires(requirePerm(global, cmds.hunger))
                        .then(literal(onName)
                            .executes(DragonsLegacyCommands::hungerOn)
                        )
                        .then(literal(offName)
                            .executes(DragonsLegacyCommands::hungerOff)
                        )
                    )
                    .then(literal(reloadName)
                        .requires(requirePerm(global, cmds.reload))
                        .executes(DragonsLegacyCommands::reload)
                    )
                    .then(literal(placeholdersName)
                        .requires(requirePerm(global, cmds.placeholders))
                        .executes(DragonsLegacyCommands::listPlaceholders)
                    )
                    // ── Admin subcommands ───────────────────────────────────
                    .then(literal("info")
                        .requires(Permissions.require(Perms.DRAGONSLEGACY_INFO, PermissionLevel.OWNERS))
                        .executes(DragonsLegacyCommands::info)
                    )
                    .then(literal("setbearer")
                        .requires(Permissions.require(Perms.DRAGONSLEGACY_SETBEARER, PermissionLevel.OWNERS))
                        .then(argument("player", EntityArgument.player())
                            .executes(DragonsLegacyCommands::setBearer)
                        )
                    )
                    .then(literal("clearability")
                        .requires(Permissions.require(Perms.DRAGONSLEGACY_CLEARABILITY, PermissionLevel.OWNERS))
                        .executes(DragonsLegacyCommands::clearAbility)
                    )
                    .then(literal("resetcooldown")
                        .requires(Permissions.require(Perms.DRAGONSLEGACY_RESETCOOLDOWN, PermissionLevel.OWNERS))
                        .executes(DragonsLegacyCommands::resetCooldown)
                    )
            );

            // Register aliases as Brigadier redirects
            var rootNode = dispatcher.getRoot().getChild(rootName);
            if (rootNode != null && cmds.aliases != null) {
                for (String alias : cmds.aliases) {
                    if (alias != null && !alias.isBlank() && !alias.equals(rootName)) {
                        dispatcher.register(literal(alias).redirect(rootNode));
                    }
                }
            }
        });
    }

    /**
     * Returns a Brigadier predicate that enforces either a LuckPerms permission node
     * or a vanilla op level, depending on the {@code permissions_api} flag in global.yaml.
     *
     * <p>When {@code permissions_api = true}: checks the configured LuckPerms permission node.
     * If no permissions API is loaded, the check is open to all (default = true).
     *
     * <p>When {@code permissions_api = false}: maps the integer op level to a {@link PermissionLevel}
     * and uses {@link Permissions#require(String, PermissionLevel)} with the op level as the fallback.
     * The configured permission node is still passed so LuckPerms can override it explicitly if desired,
     * but the op level acts as the authoritative default when no explicit node is set.
     */
    private static java.util.function.Predicate<CommandSourceStack> requirePerm(
            dev.dragonslegacy.config.GlobalConfig global,
            dev.dragonslegacy.config.GlobalConfig.CommandEntry entry) {
        if (entry == null) return src -> true;
        if (global.permissionsApi) {
            String node = entry.permissionNode;
            if (node == null || node.isBlank()) return src -> true;
            return src -> Permissions.check(src, node);
        } else {
            // permissions_api = false → use vanilla op level.
            // Map the integer op level to the nearest PermissionLevel and use Permissions.require
            // (Fabric Permissions API falls back to the op level when no permissions mod is present).
            PermissionLevel level = intToPermissionLevel(entry.opLevel);
            String node = (entry.permissionNode != null && !entry.permissionNode.isBlank())
                ? entry.permissionNode
                : "dragonslegacy.oplevel." + entry.opLevel;
            return Permissions.require(node, level);
        }
    }

    /**
     * Maps an integer operator level (0–4) to the corresponding {@link PermissionLevel} constant.
     * Values outside the 0–4 range are clamped to the nearest valid level.
     */
    private static PermissionLevel intToPermissionLevel(int opLevel) {
        return switch (Math.max(0, Math.min(4, opLevel))) {
            case 0  -> PermissionLevel.ALL;
            case 1  -> PermissionLevel.MODERATORS;
            case 2  -> PermissionLevel.GAMEMASTERS;
            case 3  -> PermissionLevel.ADMINS;
            default -> PermissionLevel.OWNERS;
        };
    }

    // =========================================================================
    // Public subcommand handlers
    // =========================================================================

    // -------------------------------------------------------------------------
    // /dragonslegacy help
    // -------------------------------------------------------------------------

    private static int help(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();
        MessageOutputSystem.send(player, messages.getEntry("help"));
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy bearer
    // -------------------------------------------------------------------------

    private static int bearer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            player.sendSystemMessage(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        UUID bearerUUID = legacy.getEggTracker().getCurrentBearer();
        if (bearerUUID == null) {
            MessageOutputSystem.send(player, messages.getEntry("bearer_none"));
        } else {
            MessageOutputSystem.send(player, messages.getEntry("bearer_info"));
        }
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy hunger on
    // -------------------------------------------------------------------------

    /**
     * Activates Dragon's Hunger for the executing player.
     *
     * <p>Only the current bearer may run this command. Non-bearers receive the
     * configurable {@code not_bearer} message.
     */
    private static int hungerOn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            player.sendSystemMessage(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        // Bearer-only enforcement
        EggTracker tracker = legacy.getEggTracker();
        UUID bearerUUID = tracker.getCurrentBearer();
        if (bearerUUID == null || !player.getUUID().equals(bearerUUID)) {
            MessageOutputSystem.send(player, messages.getEntry("not_bearer"));
            return 0;
        }

        // Activate the ability
        AbilityEngine engine = legacy.getAbilityEngine();
        engine.activateDragonHunger(player);

        // Send activation message
        MessageOutputSystem.send(player, messages.getEntry("ability_activated"));
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy hunger off
    // -------------------------------------------------------------------------

    /**
     * Deactivates Dragon's Hunger for the executing player.
     *
     * <p>Only the current bearer may run this command. Non-bearers receive the
     * configurable {@code not_bearer} message.
     */
    private static int hungerOff(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            player.sendSystemMessage(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        // Bearer-only enforcement
        EggTracker tracker = legacy.getEggTracker();
        UUID bearerUUID = tracker.getCurrentBearer();
        if (bearerUUID == null || !player.getUUID().equals(bearerUUID)) {
            MessageOutputSystem.send(player, messages.getEntry("not_bearer"));
            return 0;
        }

        // Deactivate the ability
        AbilityEngine engine = legacy.getAbilityEngine();
        engine.deactivateDragonHunger(player, "command");

        // Send deactivation message
        MessageOutputSystem.send(player, messages.getEntry("ability_deactivated"));
        return 1;
    }

    // =========================================================================
    // Admin subcommand handlers
    // =========================================================================

    // -------------------------------------------------------------------------
    // /dragonslegacy info
    // -------------------------------------------------------------------------

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        MinecraftServer server = source.getServer();
        EggTracker tracker = legacy.getEggTracker();
        AbilityEngine ability = legacy.getAbilityEngine();

        UUID bearerUUID = tracker.getCurrentBearer();
        String bearerName;
        if (bearerUUID == null) {
            bearerName = "none";
        } else {
            ServerPlayer bearerPlayer = tracker.getBearerPlayer(server);
            bearerName = bearerPlayer != null
                ? bearerPlayer.getGameProfile().name()
                : bearerUUID.toString();
        }

        EggState eggState = tracker.getCurrentState();
        String stateName = switch (eggState) {
            case HELD_BY_PLAYER -> "held";
            case PLACED_BLOCK   -> "placed";
            case DROPPED_ITEM   -> "dropped";
            case UNKNOWN        -> "unknown";
        };

        BlockPos placed = tracker.getPlacedLocation();
        String locationStr = (placed != null && eggState == EggState.PLACED_BLOCK)
            ? "x=" + placed.getX() + " y=" + placed.getY() + " z=" + placed.getZ()
            : "N/A";

        AbilityState abilityState = ability.getState();
        AbilityTimers timers = ability.getTimers();
        String abilityStatusStr = switch (abilityState) {
            case ACTIVE   -> "active";
            case COOLDOWN -> "cooldown";
            case INACTIVE -> "inactive";
        };

        MutableComponent msg = Component.empty()
            .append(header("Dragon's Legacy Status"))
            .append(field("Bearer", bearerName))
            .append(field("Egg state", stateName))
            .append(field("Egg location", locationStr))
            .append(field("Ability", abilityStatusStr));

        if (abilityState == AbilityState.ACTIVE) {
            msg.append(field("Duration remaining", timers.getDurationRemaining() + " ticks"));
        } else if (abilityState == AbilityState.COOLDOWN) {
            msg.append(field("Cooldown remaining", timers.getCooldownRemaining() + " ticks"));
        }

        source.sendSuccess(() -> msg, false);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy setbearer <player>
    // -------------------------------------------------------------------------

    private static int setBearer(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        MinecraftServer server = source.getServer();
        EggTracker tracker = legacy.getEggTracker();

        removeEggFromCurrentLocation(server, tracker);

        boolean added = target.getInventory().add(Items.DRAGON_EGG.getDefaultInstance());
        if (!added) {
            target.drop(Items.DRAGON_EGG.getDefaultInstance(), false);
        }

        tracker.updateEggHeld(target);

        String targetName = target.getGameProfile().name();
        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Bearer set to ")
                .append(Component.literal(targetName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(".")),
            true
        );
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy clearability
    // -------------------------------------------------------------------------

    private static int clearAbility(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        AbilityEngine ability = legacy.getAbilityEngine();
        AbilityState state = ability.getState();
        MinecraftServer server = source.getServer();

        if (state == AbilityState.INACTIVE) {
            source.sendFailure(Component.literal("[Dragon's Legacy] Ability is already inactive."));
            return 0;
        }

        if (state == AbilityState.ACTIVE) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ability.isActive(player)) {
                    ability.deactivateDragonHunger(player, "admin_clear");
                    break;
                }
            }
        }

        ability.resetCooldownIfNeeded();

        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Dragon's Hunger ability cleared."),
            true
        );
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy resetcooldown
    // -------------------------------------------------------------------------

    private static int resetCooldown(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        AbilityEngine ability = legacy.getAbilityEngine();
        if (ability.getState() != AbilityState.COOLDOWN) {
            source.sendFailure(Component.literal("[Dragon's Legacy] Ability is not on cooldown."));
            return 0;
        }

        ability.resetCooldownIfNeeded();
        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Dragon's Hunger cooldown reset."),
            true
        );
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy reload
    // -------------------------------------------------------------------------

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        legacy.reload(source.getServer());
        dev.dragonslegacy.features.Actions.register();
        dev.dragonslegacy.api.DragonEggAPI.init();
        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Configuration reloaded successfully."),
            true
        );
        return 1;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    // -------------------------------------------------------------------------
    // /dragonslegacy placeholders
    // -------------------------------------------------------------------------

    private static int listPlaceholders(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("[Dragon's Legacy] This command must be run by a player."));
            return -1;
        }
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        EggTracker tracker = (legacy != null) ? legacy.getEggTracker() : null;
        dev.dragonslegacy.ability.AbilityEngine ability = (legacy != null) ? legacy.getAbilityEngine() : null;

        String bearer = "none";
        String eggState = "unknown";
        if (tracker != null) {
            UUID bearerUUID = tracker.getCurrentBearer();
            if (bearerUUID != null) {
                ServerPlayer bp = DragonsLegacyMod.server != null
                    ? DragonsLegacyMod.server.getPlayerList().getPlayer(bearerUUID) : null;
                bearer = bp != null ? bp.getGameProfile().name() : bearerUUID.toString();
            }
            eggState = tracker.getCurrentState().name().toLowerCase(Locale.ROOT);
        }
        String abilityDuration = ability != null
            ? String.valueOf(ability.getTimers().getDurationRemaining()) : "0";
        String abilityCooldown = ability != null
            ? String.valueOf(ability.getTimers().getCooldownRemaining()) : "0";
        int online = DragonsLegacyMod.server != null
            ? DragonsLegacyMod.server.getPlayerList().getPlayerCount() : 0;
        int maxPlayers = DragonsLegacyMod.server != null
            ? DragonsLegacyMod.server.getPlayerList().getMaxPlayers() : 0;

        MutableComponent msg = Component.empty()
            .append(Component.literal("[Dragon's Legacy] Placeholder values:\n").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("  %dragonslegacy:player%           = " + player.getGameProfile().name() + "\n"))
            .append(Component.literal("  %dragonslegacy:executor%         = " + player.getGameProfile().name() + "\n"))
            .append(Component.literal("  %dragonslegacy:executor_uuid%    = " + player.getUUID() + "\n"))
            .append(Component.literal("  %dragonslegacy:bearer%           = " + bearer + "\n"))
            .append(Component.literal("  %dragonslegacy:global_prefix%    = (configured prefix)\n"))
            .append(Component.literal("  %dragonslegacy:egg_state%        = " + eggState + "\n"))
            .append(Component.literal("  %dragonslegacy:ability_duration% = " + abilityDuration + "\n"))
            .append(Component.literal("  %dragonslegacy:ability_cooldown% = " + abilityCooldown + "\n"))
            .append(Component.literal("  %dragonslegacy:online%           = " + online + "\n"))
            .append(Component.literal("  %dragonslegacy:max_players%      = " + maxPlayers + "\n"));
        source.sendSuccess(() -> msg, false);
        return 1;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns {@code value} if it is non-null and non-blank, otherwise returns
     * {@code fallback}.
     */
    private static String nonEmpty(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /**
     * Removes the Dragon Egg from its currently tracked location to avoid
     * duplicates when force-assigning a bearer via {@code /dragonslegacy setbearer}.
     */
    private static void removeEggFromCurrentLocation(MinecraftServer server, EggTracker tracker) {
        switch (tracker.getCurrentState()) {
            case HELD_BY_PLAYER -> {
                UUID bearerUUID = tracker.getCurrentBearer();
                if (bearerUUID == null) break;
                ServerPlayer bearer = server.getPlayerList().getPlayer(bearerUUID);
                if (bearer == null) break;
                removeEggFromInventory(bearer);
            }
            case PLACED_BLOCK -> {
                BlockPos pos = tracker.getPlacedLocation();
                if (pos == null) break;
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.getBlockState(pos).is(Blocks.DRAGON_EGG)) {
                        level.removeBlock(pos, false);
                        break;
                    }
                }
            }
            case DROPPED_ITEM -> {
                for (ServerLevel level : server.getAllLevels()) {
                    net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
                    net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                        border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                        border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                        if (item.getItem().is(Items.DRAGON_EGG)) {
                            item.discard();
                            return;
                        }
                    }
                }
            }
            default -> {} // UNKNOWN – nothing to remove
        }
    }

    /**
     * Removes one Dragon Egg stack from the player's inventory (main, armour, and offhand).
     */
    private static void removeEggFromInventory(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(Items.DRAGON_EGG)) {
                stack.shrink(stack.getCount());
                return;
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(Items.DRAGON_EGG)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static MutableComponent header(String text) {
        return Component.literal("[" + text + "]")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
    }

    private static MutableComponent field(String name, String value) {
        return Component.literal("\n  " + name + ": ")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }
}

