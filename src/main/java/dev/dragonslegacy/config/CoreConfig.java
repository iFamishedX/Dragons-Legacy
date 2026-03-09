package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Minimal core configuration loaded from {@code config/dragonslegacy/config.yaml}.
 * Contains only the mod's master on/off switch and config version.
 */
@ConfigSerializable
public class CoreConfig {

    @Setting("config_version")
    public int configVersion = 1;

    public boolean enabled = true;
}
