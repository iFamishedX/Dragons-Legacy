package dev.dragonslegacy.config;

import dev.dragonslegacy.api.DragonEggAPI.PositionType;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.dragonslegacy.DragonsLegacyMod.*;

@ConfigSerializable
public class Config {
    public static final Map<PositionType, VisibilityType> defaultVisibility = Map.of(
        PositionType.BLOCK, VisibilityType.RANDOMIZED,
        PositionType.ITEM, VisibilityType.EXACT,
        PositionType.FALLING_BLOCK, VisibilityType.EXACT,
        PositionType.INVENTORY, VisibilityType.EXACT,
        PositionType.ENTITY, VisibilityType.EXACT,
        PositionType.PLAYER, VisibilityType.HIDDEN
    );
    private static final Path PATH = CONFIG_DIR.resolve("config.conf");
    private static final HoconConfigurationLoader LOADER = HoconConfigurationLoader.builder()
        .path(PATH)
        .prettyPrinting(true)
        .defaultOptions(opts -> opts.serializers(build ->
            build
                .register(MessageString.class, new MessageString.Serializer(Messages.PARSER))
                .register(Action.class, Action.Serializer.INSTANCE)
                .register(CommandTemplate.class, CommandTemplate.Serializer.INSTANCE)
                .register(Condition.class, Condition.Serializer.INSTANCE)
        ))
        .build();

    @Comment("The radius that is used to randomize the dragon egg position.\nDefault: 25")
    public float searchRadius = 25;
    @Comment("Whether to prevent the Dragon Egg from entering an Ender Chest. " +
             "\nThis will also check the contents of container items like Shulker Boxes and Bundles.")
    public boolean blockEnderChest = true;
    @Comment("Whether to prevent the Dragon Egg from entering any container item (portable container)," +
             " e.g. Shulker Boxes and Bundles." +
             "\nThis is ignored in the creative inventory and does not check container items.")
    public boolean blockContainerItems = false;
    @Comment("Messages used throughout the mod")
    public Messages messages = new Messages();
    @Comment("The distance in blocks around the Dragon Egg where players count as 'nearby'")
    public int nearbyRange = 64;
    @Comment("""
        Actions that are executed on certain triggers.
        You can specify different actions and also specify a condition that must be met for the action to run.
        
        You can read more about how to use Actions and what you can do with them in the wiki:
        https://github.com/iFamishedX/DragonEggGame/wiki
        """
    )
    public List<Action> actions = List.of();
    @Comment(
        """
            The visibility of the dragon egg for each position type.
            Default: {
               BLOCK=RANDOMIZED, // placed as Block
               ITEM=EXACT, // item entity
               FALLING_BLOCK=EXACT, // falling block entity
               INVENTORY=EXACT, // block inventory
               ENTITY=EXACT, // entity inventory
               PLAYER=HIDDEN, // player inventory
            }"""
    )
    private Map<PositionType, VisibilityType> visibility = defaultVisibility;

    public static Config loadAndUpdateOrCreate() {
        Config config = new Config();
        if (!PATH.toFile().isFile()) {
            config.save();
            return config;
        }

        try {
            CommentedConfigurationNode node = LOADER.load();

            boolean update = false;
            if (!node.hasChild("actions")) {
                // visibility type NONE was removed
                node.node("visibility").removeChild("NONE");
                update = true;
            }
            if (!node.hasChild("block-ender-chest")) {
                // blockEnderChest and blockContainerItems were added
                update = true;
            }

            config = node.get(Config.class);

            if (update) {
                LOGGER.info("Detected old config file format, updating...");
                try {
                    Objects.requireNonNull(config).save();
                    LOGGER.info("Config file was updated to current format");
                } catch (Exception e) {
                    LOGGER.warn("Failed to update config file to new format", e);
                }
            }
        } catch (Exception e) {
            if (CONFIG != null) {
                LOGGER.warn("Failed to load config, using previous value instead", e);
                config = CONFIG;
            }
            LOGGER.warn("Failed to load config, using default config instead", e);
        }

        return config;
    }

    public boolean save() {
        CommentedConfigurationNode node = LOADER.createNode();
        try {
            node.set(this);
            LOADER.save(node);
        } catch (Exception e) {
            LOGGER.warn("Failed to save config to disk", e);
            return false;
        }
        return true;
    }

    public VisibilityType getVisibility(@Nullable PositionType type) {
        return visibility.getOrDefault(type, defaultVisibility.getOrDefault(type, VisibilityType.HIDDEN));
    }

    public enum VisibilityType {
        RANDOMIZED,
        EXACT,
        HIDDEN
    }

    @ConfigSerializable
    public static class Messages {
        private static final NodeParser PARSER = NodeParser
            .builder()
            .globalPlaceholders()
            .quickText()
            .staticPreParsing()
            .build();

        @Comment("The message that is displayed when using '/dragon_egg bearer' and the visibility of the current " +
                 "position type is 'EXACT' (see visibility)")
        public MessageString bearerExact = new MessageString(
            PARSER,
            "<yellow>The %deg:item% is currently held by <gold>%deg:bearer%</> and was last seen at " +
            "<gold>%deg:pos%</>."
        );
        @Comment("The message that is displayed when using '/dragon_egg bearer' and the visibility of the current " +
                 "position type is 'RANDOMIZED' (see visibility)")
        public MessageString bearerRandomized = new MessageString(
            PARSER,
            "<yellow>The %deg:item% is currently held by <gold>%deg:bearer%</> and was last seen around " +
            "<gold>%deg:pos%</>."
        );
        @Comment("The message that is displayed when using '/dragon_egg bearer' and the visibility of the current " +
                 "position type is 'HIDDEN' (see visibility)")
        public MessageString bearerHidden = new MessageString(
            PARSER,
            "<yellow>The %deg:item% is currently held by <gold>%deg:bearer%</>."
        );
        @Comment("The message that is displayed when using '/dragon_egg bearer' but there is no bearer")
        public MessageString noBearer = new MessageString(PARSER, "<yellow>No one has snatched the %deg:item% yet.");
        @Comment("The message that is displayed when using '/dragon_egg bearer' but there is an error")
        public MessageString bearerError = new MessageString(PARSER, "<red>Currently not available.");
        @Comment("The dragon egg bearer has changed")
        public MessageString bearerChanged = new MessageString(
            PARSER,
            "<yellow><gold>%deg:bearer%</> now has the %deg:item%!"
        );
        @Comment("The message that is displayed when using '/dragon_egg info'")
        public MessageString info = new MessageString(
            PARSER,
            """
                
                
                
                <aqua><bold>The Dragon Egg Server Game</*>
                <gray>----------------------------</*>
                <yellow>Whoever has the %deg:item%, must place the %deg:item% <gold><hover show_text "\
                    When arriving at the base, you should quickly know where to look \
                    and the time needed for the search should be appropriate.\
                ">obvious</></> and <gold><hover show_text "\
                    You shouldn't have to destroy anything to get to the %deg:item%.\
                ">accessible for everyone</></> in the own base. \
                You can <gold><hover show_text "\
                    It's supposed to be fun for everybody, so please look out for another and fight fair. \
                    (It's best if you don't fight at all!)
                    The defense should not go beyond your own base and lost items (e.g. because of death) must be returned.\
                ">protect</></> it with traps and your own life, or put it in a huge vault, \
                but it has to be <gold><hover show_text "\
                    When arriving at the base, you should quickly know where to look \
                    and the time needed for the search should be appropriate.\
                ">obvious</></> where the %deg:item% is. \
                Everyone else now can steal the %deg:item% and has to place it in their base respectively.</*>
                <red><italic>You may only steal the egg, if the current egg bearer is online \
                or if they have been offline for at least 3 days!\
                """.replace("  ", "")
        );
    }
}

