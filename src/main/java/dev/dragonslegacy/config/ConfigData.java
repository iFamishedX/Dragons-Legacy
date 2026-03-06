package dev.dragonslegacy.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POJO that holds all Dragon's Legacy YAML configuration values.
 *
 * <p>Defaults are set inline so that a fresh {@code new ConfigData()} is always
 * safe to use without any file on disk.
 */
@ConfigSerializable
public class ConfigData {

    @Comment("""
        Announcement message templates broadcast to all online players.
        Tokens of the form ${placeholder} are substituted by the event handler.
        Available tokens per key:
          egg_picked_up            -> ${player}
          egg_placed               -> ${x}, ${y}, ${z}
          egg_teleported_to_spawn  -> ${x}, ${y}, ${z}
          bearer_changed           -> ${player}
          ability_activated        -> ${player}, ${seconds}
          ability_expired          -> ${player}
          ability_cooldown_started -> ${seconds}
        """)
    public Map<String, String> announcements = defaultAnnouncements();

    @Comment("Duration of the Dragon's Hunger ability in ticks (20 ticks = 1 second). Default: 600 (30 s).")
    public int abilityDurationTicks = 600;

    @Comment("Cooldown after Dragon's Hunger expires, in ticks. Default: 6000 (5 min).")
    public int abilityCooldownTicks = 6000;

    @Comment("Real-world days a bearer may be offline before the egg is reset. Default: 3.0")
    public double offlineResetDays = 3.0;

    @Comment("When true the mod will teleport (or spawn) the Dragon's Egg to the world spawn if it cannot be located. Default: true")
    public boolean spawnFallbackEnabled = true;

    // -------------------------------------------------------------------------
    // Default announcement templates
    // -------------------------------------------------------------------------

    private static final String PREFIX = "\u00a76[Dragon's Legacy] \u00a7f";

    static Map<String, String> defaultAnnouncements() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("egg_picked_up",            PREFIX + "${player} has picked up the Dragon's Egg!");
        map.put("egg_dropped",              PREFIX + "The Dragon's Egg has been dropped!");
        map.put("egg_placed",               PREFIX + "The Dragon's Egg has been placed at ${x}, ${y}, ${z}!");
        map.put("bearer_changed",           PREFIX + "The Dragon's Egg is now held by ${player}!");
        map.put("bearer_cleared",           PREFIX + "The Dragon's Egg has no bearer.");
        map.put("egg_teleported_to_spawn",  PREFIX + "The Dragon's Egg has been returned to spawn at ${x}, ${y}, ${z}!");
        map.put("ability_activated",        PREFIX + "${player} has activated Dragon's Hunger for ${seconds} seconds!");
        map.put("ability_expired",          PREFIX + "Dragon's Hunger has expired for ${player}!");
        map.put("ability_cooldown_started", PREFIX + "Dragon's Hunger is on cooldown for ${seconds} seconds.");
        map.put("ability_cooldown_ended",   PREFIX + "Dragon's Hunger is ready again!");
        return map;
    }
}
