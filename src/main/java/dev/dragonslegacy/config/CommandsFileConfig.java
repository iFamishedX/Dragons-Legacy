package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Command names configuration loaded from {@code config/dragonslegacy/commands.yaml}.
 */
@ConfigSerializable
public class CommandsFileConfig {

    @Setting("config_version")
    public int configVersion = 1;

    public String root = "dragonslegacy";

    public List<String> aliases = new ArrayList<>(List.of("dl"));

    public SubcommandNames subcommands = new SubcommandNames();

    @ConfigSerializable
    public static class SubcommandNames {
        public String help = "help";
        public String bearer = "bearer";
        public String hunger = "hunger";

        @Setting("hunger_on")
        public String hungerOn = "on";

        @Setting("hunger_off")
        public String hungerOff = "off";

        public String reload = "reload";
        public String test = "test";
        public String placeholders = "placeholders";
    }
}
