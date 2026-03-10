package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Global configuration loaded from {@code config/dragonslegacy/global.yaml}.
 *
 * <p>Defines root command name, aliases, and per-command enabled toggles.
 * All commands are always registered with Brigadier; permission enforcement
 * is done inside each command handler, not via {@code .requires()}.
 */
@ConfigSerializable
public class GlobalConfig {

    @Setting("config_version")
    public int configVersion = 1;

    public CommandsSection commands = new CommandsSection();

    // =========================================================================
    // CommandsSection
    // =========================================================================

    @ConfigSerializable
    public static class CommandsSection {

        public String root = "dragonslegacy";

        public List<String> aliases = new ArrayList<>(List.of("dl"));

        public CommandEntry help         = new CommandEntry(true);
        public CommandEntry bearer       = new CommandEntry(true);
        public CommandEntry hunger       = new CommandEntry(true);
        public CommandEntry reload       = new CommandEntry(true);
        public CommandEntry placeholders = new CommandEntry(true);
    }

    // =========================================================================
    // CommandEntry
    // =========================================================================

    @ConfigSerializable
    public static class CommandEntry {

        /** When {@code false}, this command returns immediately with a "disabled" message. */
        public boolean enabled = true;

        public CommandEntry() {}

        public CommandEntry(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
