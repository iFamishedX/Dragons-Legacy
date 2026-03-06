package dev.dragonslegacy.egg.event;

import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggAntiDupeEngine;
import dev.dragonslegacy.egg.EggOfflineResetManager;
import dev.dragonslegacy.egg.EggProtectionManager;
import dev.dragonslegacy.egg.EggSpawnFallback;
import dev.dragonslegacy.egg.EggTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Registers Fabric API event hooks and delegates them to the appropriate
 * Dragon's Legacy manager classes.
 *
 * <p>All hooks are server-side only and complement (not replace) the hooks
 * already registered by {@link de.arvitus.dragonegggame.Events}.
 */
public class EggEventHandler {

    /** Tick interval for periodic checks (every 100 ticks ≈ 5 seconds). */
    private static final int TICK_INTERVAL = 100;

    /** Longer interval for safety checks (every 6000 ticks = 5 minutes). */
    private static final int SAFETY_CHECK_INTERVAL = 6000;

    private EggEventHandler() {}

    /**
     * Registers all Fabric event hooks. Must be called once during mod
     * initialisation after {@link DragonsLegacy#init(net.minecraft.server.MinecraftServer)}
     * has been set up on the server.
     */
    public static void register() {
        registerServerLifecycleHooks();
        registerTickHooks();
        registerPlayerHooks();
        registerWorldHooks();
    }

    // -------------------------------------------------------------------------
    // Server lifecycle
    // -------------------------------------------------------------------------

    private static void registerServerLifecycleHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            // Initial scan and anti-dupe pass
            legacy.getEggTracker().scanAndLocateEgg(server);
            legacy.getEggAntiDupeEngine().scanAndResolveDuplicates(server);
            // Ensure the egg exists; if UNKNOWN after the scan, respawn at spawn
            legacy.getEggSpawnFallback().ensureEggExists(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy != null) legacy.shutdown();
        });
    }

    // -------------------------------------------------------------------------
    // Tick hooks
    // -------------------------------------------------------------------------

    private static void registerTickHooks() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;

            int tick = server.getTickCount();

            if (tick % TICK_INTERVAL == 0) {
                EggOfflineResetManager offlineReset = legacy.getEggOfflineResetManager();
                offlineReset.tick(server);

                EggAntiDupeEngine antiDupe = legacy.getEggAntiDupeEngine();
                antiDupe.scanAndResolveDuplicates(server);
            }

            if (tick % SAFETY_CHECK_INTERVAL == 0) {
                EggProtectionManager protection = legacy.getEggProtectionManager();
                protection.verifyEggSafety(server);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Player hooks
    // -------------------------------------------------------------------------

    private static void registerPlayerHooks() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            legacy.getEggOfflineResetManager().onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            legacy.getEggOfflineResetManager().onPlayerLeave(player);
        });
    }

    // -------------------------------------------------------------------------
    // World / block hooks
    // -------------------------------------------------------------------------

    private static void registerWorldHooks() {
        // Track when a dragon egg block is broken
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!state.is(Blocks.DRAGON_EGG)) return;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;

            EggTracker tracker = legacy.getEggTracker();
            // After breaking, the egg becomes an item entity; let the entity-load event handle the state
            // For now just scan to update state
            if (player instanceof ServerPlayer serverPlayer) {
                tracker.scanAndLocateEgg(serverPlayer.server);
            }
        });

        // Watch item entities that are dying (lava, void, despawn) and protect egg
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register(
            (entity, world) -> {
                if (!(entity instanceof ItemEntity item)) return;
                if (!item.getItem().is(Items.DRAGON_EGG)) return;
                if (!item.isRemoved()) return; // only care about actual removal

                DragonsLegacy legacy = DragonsLegacy.getInstance();
                if (legacy == null) return;

                // Only protect the item if it is the canonical egg
                if (!legacy.getEggIdentityManager().isCanonicalEgg(item.getItem())) return;

                EggProtectionManager protection = legacy.getEggProtectionManager();
                protection.onEggItemEntityAboutToDie(item);
            }
        );
    }
}
