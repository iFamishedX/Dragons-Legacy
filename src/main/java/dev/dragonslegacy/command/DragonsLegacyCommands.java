package dev.dragonslegacy.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dragonslegacy.Perms;
import dev.dragonslegacy.ability.AbilityEngine;
import dev.dragonslegacy.ability.AbilityState;
import dev.dragonslegacy.ability.AbilityTimers;
import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggState;
import dev.dragonslegacy.egg.EggTracker;
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

import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Registers and handles the {@code /dragonslegacy} admin command tree.
 *
 * <p>Commands provided:
 * <ul>
 *   <li>{@code /dragonslegacy info} – Display full egg, bearer, and ability status.</li>
 *   <li>{@code /dragonslegacy setbearer <player>} – Force a player to become the bearer.</li>
 *   <li>{@code /dragonslegacy clearability} – Deactivate the Dragon's Hunger ability.</li>
 *   <li>{@code /dragonslegacy resetcooldown} – Reset the Dragon's Hunger cooldown.</li>
 * </ul>
 *
 * <p>All commands require operator-level permission ({@link PermissionLevel#OWNERS}).
 */
public class DragonsLegacyCommands {

    private DragonsLegacyCommands() {}

    /**
     * Registers the {@code /dragonslegacy} command tree with the Fabric command dispatcher.
     * Must be called during mod initialisation (before the server starts).
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                literal("dragonslegacy")
                    .requires(Permissions.require(Perms.DRAGONSLEGACY, PermissionLevel.OWNERS))
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
                    .then(literal("reload")
                        .requires(Permissions.require(Perms.DRAGONSLEGACY_RELOAD, PermissionLevel.OWNERS))
                        .executes(DragonsLegacyCommands::reload)
                    )
            )
        );
    }

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

        // Bearer
        UUID bearerUUID = tracker.getCurrentBearer();
        String bearerName;
        if (bearerUUID == null) {
            bearerName = "none";
        } else {
            ServerPlayer bearerPlayer = tracker.getBearerPlayer(server);
            bearerName = bearerPlayer != null
                ? bearerPlayer.getGameProfile().getName()
                : bearerUUID.toString();
        }

        // Egg state
        EggState eggState = tracker.getCurrentState();
        String stateName = switch (eggState) {
            case HELD_BY_PLAYER -> "held";
            case PLACED_BLOCK   -> "placed";
            case DROPPED_ITEM   -> "dropped";
            case UNKNOWN        -> "unknown";
        };

        // Egg location (only meaningful when placed as a block)
        BlockPos placed = tracker.getPlacedLocation();
        String locationStr = (placed != null && eggState == EggState.PLACED_BLOCK)
            ? "x=" + placed.getX() + " y=" + placed.getY() + " z=" + placed.getZ()
            : "N/A";

        // Ability
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

        // Remove the egg from its current tracked location before reassigning
        removeEggFromCurrentLocation(server, tracker);

        // Give the egg to the target; drop at their feet if inventory is full
        boolean added = target.getInventory().add(Items.DRAGON_EGG.getDefaultInstance());
        if (!added) {
            target.drop(Items.DRAGON_EGG.getDefaultInstance(), false);
        }

        // Update the tracker so events (bearer-changed, etc.) fire correctly
        tracker.updateEggHeld(target);

        String targetName = target.getGameProfile().getName();
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

        // If active, find the player and deactivate; this transitions state to COOLDOWN
        if (state == AbilityState.ACTIVE) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ability.isActive(player)) {
                    ability.deactivateDragonHunger(player, "admin_clear");
                    break;
                }
            }
        }

        // Clear any remaining cooldown so the state returns to INACTIVE
        ability.resetCooldownIfNeeded();

        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Dragon's Hunger ability cleared."),
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

        legacy.reload();
        source.sendSuccess(
            () -> Component.literal("[Dragon's Legacy] Configuration reloaded successfully."),
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
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Removes the Dragon Egg from its current tracked location so that reassigning
     * the bearer via {@code /dragonslegacy setbearer} does not create duplicates.
     *
     * @param server  the running {@link MinecraftServer}
     * @param tracker the current {@link EggTracker}
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
                    for (ItemEntity item : level.getEntitiesOfClass(
                            ItemEntity.class, level.getWorldBorder().createBoundingBox())) {
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
     * Removes one Dragon Egg stack from the player's inventory
     * (main slots, armour slots, and offhand).
     *
     * @param player the {@link ServerPlayer} whose inventory should be cleared
     */
    private static void removeEggFromInventory(ServerPlayer player) {
        var inv = player.getInventory();
        for (List<ItemStack> slots : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack stack : slots) {
                if (stack.is(Items.DRAGON_EGG)) {
                    stack.shrink(stack.getCount());
                    return;
                }
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
