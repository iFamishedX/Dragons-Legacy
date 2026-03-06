package dev.dragonslegacy.announce;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.ability.event.AbilityActivatedEvent;
import dev.dragonslegacy.ability.event.AbilityCooldownEndedEvent;
import dev.dragonslegacy.ability.event.AbilityCooldownStartedEvent;
import dev.dragonslegacy.ability.event.AbilityExpiredEvent;
import dev.dragonslegacy.egg.event.DragonEggEventBus;
import dev.dragonslegacy.egg.event.EggBearerChangedEvent;
import dev.dragonslegacy.egg.event.EggDroppedEvent;
import dev.dragonslegacy.egg.event.EggPickedUpEvent;
import dev.dragonslegacy.egg.event.EggPlacedEvent;
import dev.dragonslegacy.egg.event.EggTeleportedToSpawnEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central controller for all Dragon's Legacy server-wide announcements.
 *
 * <p>Subscribe to all relevant events on the {@link DragonEggEventBus}, format
 * announcement templates using {@link AnnouncementFormatter}, and broadcast the
 * result to every online player via
 * {@link net.minecraft.server.players.PlayerList#broadcastSystemMessage}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Call {@link #init(MinecraftServer, DragonEggEventBus)} once after both the
 *       server and {@link dev.dragonslegacy.egg.DragonsLegacy} have been initialised.</li>
 * </ol>
 *
 * <h3>Phase 5 migration note</h3>
 * Templates are currently hardcoded in {@link AnnouncementTemplates}.  In Phase 5
 * the templates field can be replaced with a YAML-backed implementation without
 * changing any subscriber logic here.
 */
public class AnnouncementManager {

    /** Ticks per second in vanilla Minecraft. Used to convert tick durations to seconds. */
    private static final int TICKS_PER_SECOND = 20;

    private @Nullable MinecraftServer server;

    /**
     * Runtime announcement templates, populated from {@link dev.dragonslegacy.config.ConfigManager}
     * after config is loaded.  Falls back to the hardcoded {@link AnnouncementTemplates} constants
     * when a key is absent.
     */
    private Map<String, String> templates = new HashMap<>();

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Replaces the current template map.  Called by the Dragon's Legacy coordinator
     * whenever the config is (re-)loaded.
     *
     * @param templates the new template map (may be {@code null} to reset to defaults)
     */
    public void setTemplates(Map<String, String> templates) {
        this.templates = templates != null ? templates : new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises the manager by subscribing to all relevant events on
     * {@code bus}.
     *
     * @param server the running {@link MinecraftServer}
     * @param bus    the shared {@link DragonEggEventBus}
     */
    public void init(MinecraftServer server, DragonEggEventBus bus) {
        this.server = server;

        // Egg events
        bus.subscribe(EggPickedUpEvent.class,        this::onEggPickedUp);
        bus.subscribe(EggDroppedEvent.class,         this::onEggDropped);
        bus.subscribe(EggPlacedEvent.class,          this::onEggPlaced);
        bus.subscribe(EggBearerChangedEvent.class,   this::onBearerChanged);
        bus.subscribe(EggTeleportedToSpawnEvent.class, this::onEggTeleportedToSpawn);

        // Ability events
        bus.subscribe(AbilityActivatedEvent.class,      this::onAbilityActivated);
        bus.subscribe(AbilityExpiredEvent.class,        this::onAbilityExpired);
        bus.subscribe(AbilityCooldownStartedEvent.class, this::onCooldownStarted);
        bus.subscribe(AbilityCooldownEndedEvent.class,  this::onCooldownEnded);

        DragonsLegacyMod.LOGGER.info("[Dragon's Legacy] AnnouncementManager initialised.");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Broadcasts a pre-formatted message to every online player and the server
     * console.
     *
     * @param message the fully formatted message string (may contain § color codes)
     */
    public void broadcast(String message) {
        if (server == null) return;
        Component component = Component.literal(message);
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    /**
     * Formats {@code template} by substituting all {@code ${key}} tokens with
     * values from {@code placeholders}.
     *
     * @param template     the message template
     * @param placeholders the placeholder key–value pairs
     * @return the formatted string
     */
    public String format(String template, Map<String, String> placeholders) {
        return AnnouncementFormatter.format(template, placeholders);
    }

    // -------------------------------------------------------------------------
    // Egg event handlers
    // -------------------------------------------------------------------------

    private void onEggPickedUp(EggPickedUpEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", event.getPlayer().getGameProfile().name());
        broadcast(format(getTemplate("egg_picked_up", AnnouncementTemplates.EGG_PICKED_UP), ph));
    }

    private void onEggDropped(EggDroppedEvent event) {
        broadcast(getTemplate("egg_dropped", AnnouncementTemplates.EGG_DROPPED));
    }

    private void onEggPlaced(EggPlacedEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("x", String.valueOf(event.getPosition().getX()));
        ph.put("y", String.valueOf(event.getPosition().getY()));
        ph.put("z", String.valueOf(event.getPosition().getZ()));
        broadcast(format(getTemplate("egg_placed", AnnouncementTemplates.EGG_PLACED), ph));
    }

    private void onBearerChanged(EggBearerChangedEvent event) {
        UUID newBearer = event.getNewBearerUUID();
        if (newBearer == null) {
            broadcast(getTemplate("bearer_cleared", AnnouncementTemplates.BEARER_CLEARED));
        } else {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", resolvePlayerName(newBearer));
            broadcast(format(getTemplate("bearer_changed", AnnouncementTemplates.BEARER_CHANGED), ph));
        }
    }

    private void onEggTeleportedToSpawn(EggTeleportedToSpawnEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("x", String.valueOf((int) event.getSpawnPosition().x));
        ph.put("y", String.valueOf((int) event.getSpawnPosition().y));
        ph.put("z", String.valueOf((int) event.getSpawnPosition().z));
        broadcast(format(getTemplate("egg_teleported_to_spawn", AnnouncementTemplates.EGG_TELEPORTED_TO_SPAWN), ph));
    }

    // -------------------------------------------------------------------------
    // Ability event handlers
    // -------------------------------------------------------------------------

    private void onAbilityActivated(AbilityActivatedEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", resolvePlayerName(event.getPlayerUUID()));
        ph.put("seconds", String.valueOf(event.getDuration() / TICKS_PER_SECOND));
        broadcast(format(getTemplate("ability_activated", AnnouncementTemplates.ABILITY_ACTIVATED), ph));
    }

    private void onAbilityExpired(AbilityExpiredEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", resolvePlayerName(event.getPlayerUUID()));
        broadcast(format(getTemplate("ability_expired", AnnouncementTemplates.ABILITY_EXPIRED), ph));
    }

    private void onCooldownStarted(AbilityCooldownStartedEvent event) {
        Map<String, String> ph = new HashMap<>();
        ph.put("seconds", String.valueOf(event.getCooldownTicks() / TICKS_PER_SECOND));
        broadcast(format(getTemplate("ability_cooldown_started", AnnouncementTemplates.ABILITY_COOLDOWN_STARTED), ph));
    }

    private void onCooldownEnded(AbilityCooldownEndedEvent event) {
        broadcast(getTemplate("ability_cooldown_ended", AnnouncementTemplates.ABILITY_COOLDOWN_ENDED));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the template for {@code key} from the runtime map, or
     * {@code fallback} (the hardcoded constant) if absent.
     */
    private String getTemplate(String key, String fallback) {
        String value = templates.get(key);
        return value != null ? value : fallback;
    }

    /**
     * Resolves a player's display name from their UUID.
     *
     * <p>If the player is online their current display name is used.
     * If they are offline the UUID string is returned as a fallback so that
     * announcements are never blocked by offline-player lookups.
     *
     * @param uuid the player UUID to resolve
     * @return the player's name, or the UUID string if unknown
     */
    private String resolvePlayerName(UUID uuid) {
        if (server == null) return uuid.toString();
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile().name();
        // Offline fallback: try the server's name-to-id resolver cache
        try {
            net.minecraft.server.players.CachedUserNameToIdResolver userCache =
                (net.minecraft.server.players.CachedUserNameToIdResolver) server.services().nameToIdCache();
            if (userCache != null) {
                var result = userCache.get(uuid);
                if (result.isPresent()) return result.get().name();
            }
        } catch (Exception ignored) {}
        return uuid.toString();
    }
}
