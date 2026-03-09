package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Glow system configuration loaded from {@code config/dragonslegacy/glow.yaml}.
 */
@ConfigSerializable
public class GlowConfig {

    @Setting("config_version")
    public int configVersion = 1;

    public GlowSection glow = new GlowSection();

    @ConfigSerializable
    public static class GlowSection {
        public boolean enabled = true;
        public String color = "#FFFFFF";
        public CraftingSection crafting = new CraftingSection();

        @ConfigSerializable
        public static class CraftingSection {
            public boolean enabled = true;
            public String type = "anvil";

            @Setting("base_item")
            public String baseItem = "minecraft:dragon_egg";

            public Map<String, String> materials = buildDefaultMaterials();

            private static Map<String, String> buildDefaultMaterials() {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("amethyst_shard", "#AA00FF");
                map.put("copper_ingot",   "#B87333");
                map.put("gold_ingot",     "#FFD700");
                map.put("iron_ingot",     "#D8D8D8");
                map.put("netherite_ingot","#3C2A23");
                map.put("quartz",         "#E7E7E7");
                map.put("redstone",       "#FF0000");
                map.put("emerald",        "#00FF55");
                map.put("diamond",        "#00FFFF");
                return map;
            }
        }
    }
}
