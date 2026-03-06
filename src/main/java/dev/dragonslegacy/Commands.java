package dev.dragonslegacy;

import com.mojang.brigadier.context.CommandContext;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.config.Config;
import dev.dragonslegacy.config.Data;
import dev.dragonslegacy.config.MessageString;
import dev.dragonslegacy.features.Actions;
import dev.dragonslegacy.utils.CommandNode;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static dev.dragonslegacy.DragonsLegacyMod.CONFIG;
import static dev.dragonslegacy.DragonsLegacyMod.LOGGER;

public class Commands {
    private static final List<CommandNode> ALL = new ArrayList<>();

    static {
        add(
            new CommandNode(DragonsLegacyMod.MOD_ID_ALIAS, "Get info about the mod", Commands::deg$info)
                .withPermission(Perms.MOD_INFO, PermissionLevel.OWNERS)
                .addChild(new CommandNode("reload", "Reload config and data", Commands::reload)
                    .withPermission(Perms.RELOAD, PermissionLevel.OWNERS)
                )
        );
        add(
            new CommandNode("dragon_egg")
                .withOptionalPermission(Perms.BASE)
                .addChild(new CommandNode(
                        "bearer",
                        "Get info about the current bearer of the Dragon Egg",
                        Commands::dragon_egg$bearer
                    )
                        .withOptionalPermission(Perms.BEARER)
                )
                .addChild(new CommandNode("info", "Get info about the Dragon Egg game", Commands::dragon_egg$info)
                    .withOptionalPermission(Perms.INFO)
                )
                .addChild(new CommandNode("help", "View all commands", Commands::dragon_egg$help)
                    .withOptionalPermission(Perms.HELP)
                )
        );
    }

    public static void add(CommandNode node) {
        ALL.add(node);
    }

    public static List<CommandNode> get() {
        return List.copyOf(ALL);
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            for (CommandNode node : ALL)
                dispatcher.register(node.build());
        });
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (!reload()) {
            context.getSource().sendFailure(Component.literal(
                "Failed to load config, using previous value instead. See console for more information."));
            return -1;
        }
        context.getSource()
            .sendSuccess(() -> Component.literal("Reloaded Dragon's Legacy config and data"), false);
        return 1;
    }

    public static boolean reload() {
        Config oldConfig = CONFIG;
        CONFIG = Config.loadAndUpdateOrCreate();
        if (CONFIG == oldConfig) {
            LOGGER.error("Failed to load config, using previous value instead.");
            return false;
        }
        Actions.register();
        DragonEggAPI.init();
        LOGGER.info("Reloaded Dragon's Legacy config and data");
        return true;
    }

    private static int deg$info(CommandContext<CommandSourceStack> context) {
        FabricLoader.getInstance().getModContainer(DragonsLegacyMod.MOD_ID).ifPresent(modContainer -> {
            ModMetadata meta = modContainer.getMetadata();
            context.getSource().sendSuccess(
                () ->
                    Component.literal(
                        String.format(
                            "%s v%s by %s",
                            meta.getName(),
                            meta.getVersion(),
                            meta.getAuthors().stream().findFirst().isEmpty()
                                ? "Unknown"
                                : meta.getAuthors().stream().findFirst().get().getName()
                        )
                    ).copy().setStyle(
                        Style.EMPTY
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(meta
                                .getContact()
                                .get("source")
                                .orElse("https://github.com/iFamishedX/DragonEggGame")
                            )))
                            .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to view source")
                            ))),
                false
            );
        });
        return 0;
    }

    private static int dragon_egg$bearer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Data data = DragonEggAPI.getData();
        if (data == null) {
            source.sendFailure(CONFIG.messages.bearerError.node.toText(PlaceholderContext.of(
                source.withMaximumPermission(LevelBasedPermissionSet.OWNER)
            )));
            return -1;
        }

        MessageString message;
        if (data.playerUUID == null) message = CONFIG.messages.noBearer;
        else message = switch (CONFIG.getVisibility(data.type)) {
            case EXACT -> CONFIG.messages.bearerExact;
            case RANDOMIZED -> CONFIG.messages.bearerRandomized;
            case HIDDEN -> CONFIG.messages.bearerHidden;
        };

        TextNode node = message.node;
        source.sendSuccess(
            () -> node.toText(PlaceholderContext.of(source.withMaximumPermission(LevelBasedPermissionSet.OWNER))),
            false
        );
        return 0;
    }

    private static int dragon_egg$info(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
            () -> CONFIG.messages.info.node.toText(PlaceholderContext.of(context
                .getSource()
                .withMaximumPermission(LevelBasedPermissionSet.OWNER))),
            false
        );
        return 0;
    }

    private static int dragon_egg$help(CommandContext<CommandSourceStack> context) {
        var msg = Component.empty().append(Component
            .literal("Available commands:")
            .withStyle(Style.EMPTY.withBold(true)));
        for (CommandNode node : ALL) {
            for (CommandNode actionNode : node.getActionNodes()) {
                if (!actionNode.testCondition(context.getSource()))
                    continue;
                var cmd = net.minecraft.commands.Commands.COMMAND_PREFIX + String.join(" ", actionNode.getPath());
                var component = Component
                    .literal("\n  " + cmd)
                    .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(cmd)))
                        .withClickEvent(new ClickEvent.SuggestCommand(cmd)));
                if (actionNode.description() != null)
                    component.append(" - ").append(Component
                        .literal(actionNode.description())
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                msg.append(component);
            }
        }
        context.getSource().sendSuccess(() -> msg, false);
        return 0;
    }
}