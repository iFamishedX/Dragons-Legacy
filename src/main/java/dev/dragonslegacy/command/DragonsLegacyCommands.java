package dev.dragonslegacy.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.Perms;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.ability.AbilityState;
import dev.dragonslegacy.ability.AbilityTimers;
import dev.dragonslegacy.config.GlobalConfig;
import dev.dragonslegacy.config.MessagesConfig;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggCore;
import dev.dragonslegacy.egg.EggLocation;
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

import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Registers and handles the {@code /dragonslegacy} (alias {@code /dl}) command tree.
 *
 * <p>All commands are always registered with Brigadier — no {@code .requires()} filters
 * are applied at the node level.  Permission enforcement is performed at the start of
 * each executor method so that the console, operators, and permission-plugin users all
 * receive clear feedback instead of a silent "unknown command".
 *
 * <h3>Public subcommands (available to all players and console)</h3>
 * <ul>
 *   <li>{@code /dragonslegacy help}         – Display configurable help message.</li>
 *   <li>{@code /dragonslegacy bearer}       – Show the current egg bearer.</li>
 *   <li>{@code /dragonslegacy hunger on}    – Activate Dragon's Hunger (bearer only).</li>
 *   <li>{@code /dragonslegacy hunger off}   – Deactivate Dragon's Hunger (bearer only).</li>
 *   <li>{@code /dragonslegacy placeholders} – List all placeholder values.</li>
 * </ul>
 *
 * <h3>Admin subcommands (require {@code dragonslegacy.admin} or op level 3+)</h3>
 * <ul>
 *   <li>{@code /dragonslegacy info}               – Full egg/ability status.</li>
 *   <li>{@code /dragonslegacy setbearer <player>} – Force-assign the bearer.</li>
 *   <li>{@code /dragonslegacy clearability}       – Deactivate the ability.</li>
 *   <li>{@code /dragonslegacy resetcooldown}      – Reset the cooldown.</li>
 *   <li>{@code /dragonslegacy reload}             – Reload all configs.</li>
 *   <li>{@code /dragonslegacy papitest}           – Test PlaceholderAPI integration.</li>
 * </ul>
 */
public class DragonsLegacyCommands {

    // Hard-coded admin permission node used for all operator-only subcommands.
    private static final String PERM_ADMIN  = "dragonslegacy.admin";
    // Per-subcommand permission nodes (fall back to PERM_ADMIN when not explicitly granted).
    private static final String PERM_RELOAD      = "dragonslegacy.command.reload";
    private static final String PERM_PAPITEST    = "dragonslegacy.command.papitest";

    private DragonsLegacyCommands() {}

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers the {@code /dragonslegacy} command tree (and its configured aliases)
     * with the Fabric command dispatcher.  Must be called during mod initialisation.
     *
     * <p>No {@code .requires()} predicates are attached to any node — all commands are
     * always visible and reachable.  Permission checks happen inside each executor.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GlobalConfig global = DragonsLegacyMod.configManager.getGlobal();
            GlobalConfig.CommandsSection cmds =
                global.commands != null ? global.commands : new GlobalConfig.CommandsSection();

            String rootName         = nonEmpty(cmds.root, "dragonslegacy");
            String helpName         = "help";
            String bearerName       = "bearer";
            String hungerName       = "hunger";
            String onName           = "on";
            String offName          = "off";
            String reloadName       = "reload";
            String placeholdersName = "placeholders";

            // Validate messages.yaml on registration so misconfigurations are logged early.
            MessageOutputSystem.validateAll(DragonsLegacyMod.configManager.getMessages());

            // Build and register the root command — no .requires() on any node.
            dispatcher.register(
                literal(rootName)
                    // ── Public subcommands ──────────────────────────────────
                    .then(literal(helpName)
                        .executes(DragonsLegacyCommands::help)
                    )
                    .then(literal(bearerName)
                        .executes(DragonsLegacyCommands::bearer)
                    )
                    .then(literal(hungerName)
                        .then(literal(onName)
                            .executes(DragonsLegacyCommands::hungerOn)
                        )
                        .then(literal(offName)
                            .executes(DragonsLegacyCommands::hungerOff)
                        )
                    )
                    .then(literal(reloadName)
                        .executes(DragonsLegacyCommands::reload)
                    )
                    .then(literal(placeholdersName)
                        .executes(DragonsLegacyCommands::listPlaceholders)
                    )
                    // ── Admin subcommands ───────────────────────────────────
                    .then(literal("info")
                        .executes(DragonsLegacyCommands::info)
                    )
                    .then(literal("setbearer")
                        .then(argument("player", EntityArgument.player())
                            .executes(DragonsLegacyCommands::setBearer)
                        )
                    )
                    .then(literal("clearability")
                        .executes(DragonsLegacyCommands::clearAbility)
                    )
                    .then(literal("resetcooldown")
                        .executes(DragonsLegacyCommands::resetCooldown)
                    )
                    .then(literal("papitest")
                        .executes(DragonsLegacyCommands::papiTest)
                    )
            );

            // Register aliases as Brigadier redirects.
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

    // -------------------------------------------------------------------------
    // Permission helper
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code source} has the given LuckPerms/Fabric permission
     * node, or falls back to the vanilla operator level when no permission plugin is loaded.
     *
     * @param source    the command source to check
     * @param node      the permission node (e.g. {@code "dragonslegacy.admin"})
     * @param opLevel   vanilla op level used as fallback (0 = everyone, 3 = admins, 4 = owners)
     */
    private static boolean hasPerm(CommandSourceStack source, String node, int opLevel) {
        PermissionLevel level = switch (Math.max(0, Math.min(4, opLevel))) {
            case 0  -> PermissionLevel.ALL;
            case 1  -> PermissionLevel.MODERATORS;
            case 2  -> PermissionLevel.GAMEMASTERS;
            case 3  -> PermissionLevel.ADMINS;
            default -> PermissionLevel.OWNERS;
        };
        return Permissions.check(source, node, level);
    }

    /** Checks {@code dragonslegacy.admin} with op-level 3 fallback. */
    private static boolean isAdmin(CommandSourceStack source) {
        return hasPerm(source, PERM_ADMIN, 3);
    }

    // =========================================================================
    // Public subcommand handlers
    // =========================================================================

    // -------------------------------------------------------------------------
    // /dragonslegacy help
    // -------------------------------------------------------------------------

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GlobalConfig.CommandEntry entry = commandEntry(e -> e.help);
        if (!isCommandEnabled(source, entry)) return 0;

        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();
        ServerPlayer player = tryGetPlayer(source);
        if (player != null) {
            MessageOutputSystem.send(player, messages.getEntry("help"));
        } else {
            // Console: send the raw help text directly
            source.sendSuccess(() -> Component.literal(
                "[Dragon's Legacy] help | bearer | hunger on/off | placeholders | reload | info | setbearer | clearability | resetcooldown | papitest"), false);
        }
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy bearer
    // -------------------------------------------------------------------------

    private static int bearer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GlobalConfig.CommandEntry entry = commandEntry(e -> e.bearer);
        if (!isCommandEnabled(source, entry)) return 0;

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        EggCore eggCore = legacy.getEggCore();
        UUID bearerUUID = eggCore.getBearerUuid().orElse(null);
        ServerPlayer player = tryGetPlayer(source);

        if (player != null) {
            if (bearerUUID == null) {
                MessageOutputSystem.send(player, messages.getEntry("bearer_none"));
            } else {
                MessageOutputSystem.send(player, messages.getEntry("bearer_info"));
            }
        } else {
            // Console output
            if (bearerUUID == null) {
                source.sendSuccess(() -> Component.literal("[Dragon's Legacy] No one holds the Dragon Egg yet."), false);
            } else {
                ServerPlayer bearerPlayer = eggCore.getBearerPlayer(source.getServer()).orElse(null);
                String name = bearerPlayer != null ? bearerPlayer.getGameProfile().name() : bearerUUID.toString();
                source.sendSuccess(() -> Component.literal("[Dragon's Legacy] Bearer: " + name), false);
            }
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
     * configurable {@code not_bearer} message.  Console callers are rejected.
     */
    private static int hungerOn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GlobalConfig.CommandEntry entry = commandEntry(e -> e.hunger);
        if (!isCommandEnabled(source, entry)) return 0;

        ServerPlayer player = tryGetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] /hunger must be run by a player."));
            return -1;
        }

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            player.sendSystemMessage(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        // Bearer-only enforcement: must hold the egg and be the bearer
        EggCore eggCore = legacy.getEggCore();
        UUID bearerUUID = eggCore.getBearerUuid().orElse(null);
        if (bearerUUID == null || !player.getUUID().equals(bearerUUID)
                || eggCore.getEggState() != EggState.PLAYER) {
            MessageOutputSystem.send(player, messages.getEntry("not_bearer"));
            return 0;
        }

        AbilityEngine engine = legacy.getAbilityEngine();
        engine.activateDragonHunger(player);
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
     * configurable {@code not_bearer} message.  Console callers are rejected.
     */
    private static int hungerOff(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GlobalConfig.CommandEntry entry = commandEntry(e -> e.hunger);
        if (!isCommandEnabled(source, entry)) return 0;

        ServerPlayer player = tryGetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] /hunger must be run by a player."));
            return -1;
        }

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        MessagesConfig messages = DragonsLegacyMod.configManager.getMessages();

        if (legacy == null) {
            player.sendSystemMessage(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        // Bearer-only enforcement
        EggCore eggCore = legacy.getEggCore();
        UUID bearerUUID = eggCore.getBearerUuid().orElse(null);
        if (bearerUUID == null || !player.getUUID().equals(bearerUUID)) {
            MessageOutputSystem.send(player, messages.getEntry("not_bearer"));
            return 0;
        }

        AbilityEngine engine = legacy.getAbilityEngine();
        engine.deactivateDragonHunger(player, "command");
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
        if (!isAdmin(source)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to run this command."));
            return 0;
        }

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        MinecraftServer server = source.getServer();
        EggCore eggCore = legacy.getEggCore();
        AbilityEngine ability = legacy.getAbilityEngine();

        String bearerName = eggCore.getBearerPlayer(server)
            .map(p -> p.getGameProfile().name())
            .orElseGet(() -> eggCore.getBearerUuid()
                .map(UUID::toString)
                .orElse("none"));

        EggState eggState = eggCore.getEggState();
        String stateName = switch (eggState) {
            case PLAYER  -> "player";
            case BLOCK   -> "block";
            case WORLD   -> "world";
            case UNKNOWN -> "unknown";
        };

        String locationStr = eggCore.getEggLocation()
            .map(loc -> loc.pos() != null
                ? "x=" + loc.pos().getX() + " y=" + loc.pos().getY() + " z=" + loc.pos().getZ()
                    + (loc.dimension() != null ? " [" + loc.dimension().identifier() + "]" : "")
                : stateName)
            .orElse("N/A");

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
        CommandSourceStack source = context.getSource();
        if (!isAdmin(source)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to run this command."));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        MinecraftServer server = source.getServer();
        EggCore eggCore = legacy.getEggCore();
        EggTracker tracker = legacy.getEggTracker();

        removeEggFromCurrentLocation(server, tracker);

        // Create a tagged egg for the new bearer
        ItemStack eggStack = Items.DRAGON_EGG.getDefaultInstance();
        EggCore.tagEgg(eggStack);
        boolean added = target.getInventory().add(eggStack);
        if (!added) {
            target.drop(eggStack, false);
        }

        eggCore.setBearer(target);

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
        if (!isAdmin(source)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to run this command."));
            return 0;
        }

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
        if (!isAdmin(source)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to run this command."));
            return 0;
        }

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
        if (!hasPerm(source, PERM_RELOAD, 3)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to reload configuration."));
            return 0;
        }

        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] System not initialised yet."));
            return -1;
        }

        legacy.reload(source.getServer());
        dev.dragonslegacy.features.Actions.register();
        dev.dragonslegacy.api.DragonEggAPI.init();
        dev.dragonslegacy.Placeholders.registerDynamic();
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
        GlobalConfig.CommandEntry entry = commandEntry(e -> e.placeholders);
        if (!isCommandEnabled(source, entry)) return 0;

        ServerPlayer player = tryGetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("[Dragon's Legacy] This command must be run by a player."));
            return -1;
        }
        DragonsLegacy legacy = DragonsLegacy.getInstance();

        MutableComponent msg = Component.empty()
            .append(Component.literal("[Dragon's Legacy] Placeholder values:\n").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("  %dragonslegacy:player%           = " + player.getGameProfile().name() + "\n"))
            .append(Component.literal("  %dragonslegacy:executor%         = " + player.getGameProfile().name() + "\n"))
            .append(Component.literal("  %dragonslegacy:executor_uuid%    = " + player.getUUID() + "\n"))
            .append(Component.literal("  %dragonslegacy:bearer%           = " + dev.dragonslegacy.api.APIUtils.getBearer() + "\n"))
            .append(Component.literal("  %dragonslegacy:global_prefix%    = "
                + DragonsLegacyMod.configManager.getMessages().prefix + "\n"));

        dev.dragonslegacy.config.PlaceholdersConfig cfg = DragonsLegacyMod.configManager.getPlaceholders();
        if (cfg != null && cfg.placeholders != null && !cfg.placeholders.isEmpty()) {
            msg.append(Component.literal("  --- config-driven placeholders ---\n").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, dev.dragonslegacy.config.PlaceholdersConfig.PlaceholderDef> e
                    : cfg.placeholders.entrySet()) {
                String name = e.getKey();
                dev.dragonslegacy.config.PlaceholdersConfig.PlaceholderDef def = e.getValue();
                if (def == null) continue;
                String value = dev.dragonslegacy.PlaceholderEngine.resolve(def, player);
                msg.append(Component.literal("  %dragonslegacy:" + name + "% = " + value + "\n"));
            }
        }

        source.sendSuccess(() -> msg, false);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /dragonslegacy papitest
    // -------------------------------------------------------------------------

    /**
     * Admin command: tests PlaceholderAPI integration by resolving all
     * {@code %dragonslegacy:*%} placeholders for the executing source and
     * displaying the results.
     */
    private static int papiTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!hasPerm(source, PERM_PAPITEST, 3)) {
            source.sendFailure(Component.literal("[Dragon's Legacy] You don't have permission to run papitest."));
            return 0;
        }

        ServerPlayer player = tryGetPlayer(source);

        MutableComponent msg = Component.empty()
            .append(Component.literal("[Dragon's Legacy] PAPI Test Results:\n").withStyle(ChatFormatting.GOLD));

        if (player != null) {
            // Static placeholders
            msg.append(Component.literal("  %dragonslegacy:player%        = " + player.getGameProfile().name() + "\n"))
               .append(Component.literal("  %dragonslegacy:bearer%        = " + dev.dragonslegacy.api.APIUtils.getBearer() + "\n"))
               .append(Component.literal("  %dragonslegacy:global_prefix% = "
                   + DragonsLegacyMod.configManager.getMessages().prefix + "\n"));
        } else {
            msg.append(Component.literal("  (running from console — player-specific placeholders unavailable)\n")
                .withStyle(ChatFormatting.GRAY));
            msg.append(Component.literal("  %dragonslegacy:bearer%        = " + dev.dragonslegacy.api.APIUtils.getBearer() + "\n"));
        }

        // Config-driven placeholders from placeholders.yaml
        dev.dragonslegacy.config.PlaceholdersConfig cfg = DragonsLegacyMod.configManager.getPlaceholders();
        if (cfg != null && cfg.placeholders != null && !cfg.placeholders.isEmpty()) {
            msg.append(Component.literal("  --- config-driven (%dragonslegacy:*%) ---\n").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, dev.dragonslegacy.config.PlaceholdersConfig.PlaceholderDef> e
                    : cfg.placeholders.entrySet()) {
                String name = e.getKey();
                dev.dragonslegacy.config.PlaceholdersConfig.PlaceholderDef def = e.getValue();
                if (def == null) continue;
                String value = (player != null)
                    ? dev.dragonslegacy.PlaceholderEngine.resolve(def, player)
                    : "(requires player)";
                msg.append(Component.literal("  %dragonslegacy:" + name + "% = " + value + "\n"));
            }
        } else {
            msg.append(Component.literal("  (no config-driven placeholders registered)\n").withStyle(ChatFormatting.GRAY));
        }

        source.sendSuccess(() -> msg, false);
        return 1;
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    /**
     * Returns {@code value} if it is non-null and non-blank, otherwise returns {@code fallback}.
     */
    private static String nonEmpty(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /**
     * Tries to get the player from {@code source}; returns {@code null} if the source is console.
     */
    private static ServerPlayer tryGetPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    /**
     * Returns the {@link GlobalConfig.CommandEntry} for the given accessor,
     * or a default (enabled) entry if the config is absent.
     */
    private static GlobalConfig.CommandEntry commandEntry(
            java.util.function.Function<GlobalConfig.CommandsSection, GlobalConfig.CommandEntry> accessor) {
        GlobalConfig global = DragonsLegacyMod.configManager.getGlobal();
        GlobalConfig.CommandsSection cmds = global != null ? global.commands : null;
        if (cmds == null) return new GlobalConfig.CommandEntry(true);
        GlobalConfig.CommandEntry entry = accessor.apply(cmds);
        return entry != null ? entry : new GlobalConfig.CommandEntry(true);
    }

    /**
     * Returns {@code false} and sends a "disabled" failure message if the command entry
     * has {@code enabled = false}.
     */
    private static boolean isCommandEnabled(CommandSourceStack source, GlobalConfig.CommandEntry entry) {
        if (entry != null && !entry.enabled) {
            source.sendFailure(Component.literal("[Dragon's Legacy] This command is currently disabled."));
            return false;
        }
        return true;
    }

    /**
     * Removes the Dragon Egg from its currently tracked location to avoid
     * duplicates when force-assigning a bearer via {@code /dragonslegacy setbearer}.
     */
    private static void removeEggFromCurrentLocation(MinecraftServer server, EggTracker tracker) {
        switch (tracker.getCurrentState()) {
            case PLAYER -> {
                UUID bearerUUID = tracker.getCurrentBearer();
                if (bearerUUID == null) break;
                ServerPlayer bearer = server.getPlayerList().getPlayer(bearerUUID);
                if (bearer == null) break;
                removeEggFromInventory(bearer);
            }
            case BLOCK -> {
                BlockPos pos = tracker.getPlacedLocation();
                if (pos == null) break;
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.getBlockState(pos).is(Blocks.DRAGON_EGG)) {
                        level.removeBlock(pos, false);
                        break;
                    }
                }
            }
            case WORLD -> {
                for (ServerLevel level : server.getAllLevels()) {
                    net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
                    net.minecraft.world.phys.AABB borderBox = new net.minecraft.world.phys.AABB(
                        border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                        border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, borderBox)) {
                        if (EggCore.isDragonEgg(item.getItem())) {
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
     * Removes one canonical Dragon Egg stack from the player's inventory
     * (main, armour, and offhand).
     */
    private static void removeEggFromInventory(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (EggCore.isDragonEgg(stack)) {
                stack.shrink(stack.getCount());
                return;
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (EggCore.isDragonEgg(stack)) {
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

