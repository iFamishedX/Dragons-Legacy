package dev.dragonslegacy.config;

import dev.dragonslegacy.api.DragonEggAPI.PositionType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Egg behavior configuration loaded from {@code config/dragonslegacy/egg.yaml}.
 */
@ConfigSerializable
public class EggConfig {

    @Setting("config_version")
    public int configVersion = 1;

    @Setting("search_radius")
    public float searchRadius = 25f;

    @Setting("block_ender_chest")
    public boolean blockEnderChest = true;

    @Setting("block_container_items")
    public boolean blockContainerItems = false;

    @Setting("offline_reset_days")
    public double offlineResetDays = 3.0;

    @Setting("nearby_range")
    public int nearbyRange = 64;

    public Map<PositionType, VisibilityType> visibility = buildDefaultVisibility();

    public ProtectionSection protection = new ProtectionSection();

    private static Map<PositionType, VisibilityType> buildDefaultVisibility() {
        Map<PositionType, VisibilityType> map = new LinkedHashMap<>();
        map.put(PositionType.INVENTORY,     VisibilityType.EXACT);
        map.put(PositionType.ITEM,          VisibilityType.EXACT);
        map.put(PositionType.PLAYER,        VisibilityType.HIDDEN);
        map.put(PositionType.BLOCK,         VisibilityType.RANDOMIZED);
        map.put(PositionType.FALLING_BLOCK, VisibilityType.EXACT);
        map.put(PositionType.ENTITY,        VisibilityType.EXACT);
        return map;
    }

    /** Anti-loss protection settings for the Dragon Egg. */
    @ConfigSerializable
    public static class ProtectionSection {
        @Setting("void")
        public boolean voidProtection = true;
        public boolean fire = true;
        public boolean lava = true;
        public boolean explosions = true;
        public boolean cactus = true;
        public boolean despawn = true;
        public boolean hopper = true;
        public boolean portal = true;
    }
}
