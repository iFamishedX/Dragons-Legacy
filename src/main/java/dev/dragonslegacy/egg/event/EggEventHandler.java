package dev.dragonslegacy.egg.event;

import dev.dragonslegacy.egg.DragonsLegacy;
import dev.dragonslegacy.egg.EggAntiDupeEngine;
import dev.dragonslegacy.egg.EggCore;
import dev.dragonslegacy.egg.EggOfflineResetManager;
import dev.dragonslegacy.egg.EggPromotion;
import dev.dragonslegacy.egg.EggProtectionManager;
import dev.dragonslegacy.egg.EggTracker;
import dev.dragonslegacy.ability.AbilityEngine;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Registers Fabric API event hooks and delegates them to the appropriate
 * Dragon's Legacy manager classes.
 *
 * <p>Egg identity is checked exclusively via {@link EggCore#isDragonEgg(ItemStack)}.
 * Promotion is handled exclusively via {@link EggPromotion} – the scrubber
 * ({@link dev.dragonslegacy.egg.EggAntiDupeEngine}) never promotes.
 */
public class EggEventHandler {

    /** Tick interval for egg heartbeat / periodic checks (every 100 ticks ≈ 5 seconds). */
    private static final int TICK_INTERVAL = 100;

    /** Longer interval for safety checks (every 6000 ticks = 5 minutes). */
    private static final int SAFETY_CHECK_INTERVAL = 6000;

    private EggEventHandler() {}

    /**
     * Registers all Fabric event hooks. Must be called once during mod
     * initialisation.
     */
    public static void register() {
        registerServerLifecycleHooks();
        registerTickHooks();
        registerPlayerHooks();
        registerWorldHooks();
        registerAbilityHooks();
    }

    // -------------------------------------------------------------------------
    // Server lifecycle
    // -------------------------------------------------------------------------

    private static void registerServerLifecycleHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            // Initial scan via EggCore
            legacy.getEggCore().tick(server);
            // Attempt item-entity fallback promotion at server start (no players online yet,
            // so player-inventory promotion cannot fire; this covers vanilla eggs that may
            // be lying in the world as item entities after a server restart).
            EggPromotion.tryPromoteAny(server);
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

            // Tick the ability engine every server tick
            legacy.getAbilityEngine().tick(server);

            boolean immediateRequested = legacy.getEggCore().consumeImmediateScrub();

            if (tick % TICK_INTERVAL == 0 || immediateRequested) {
                // Heartbeat: scan for egg via EggCore (includes fallback grace logic)
                legacy.getEggCore().tick(server);

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
            // Re-apply passive effects if this player is the current bearer
            if (player.getUUID().equals(legacy.getEggTracker().getCurrentBearer())) {
                legacy.getPassiveEffectsEngine().applyToBearer(player);
            }
            // Attempt event-driven promotion: if this player's inventory has a vanilla
            // dragon egg and no canonical ID exists yet, promote it immediately.
            EggPromotion.tryPromote(player);
            // Request immediate scrub so inventory eggs are located/cleaned before entities
            legacy.getEggCore().requestImmediateScrub();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            legacy.getEggOfflineResetManager().onPlayerLeave(player);
            // Remove passive effects when the bearer disconnects
            if (player.getUUID().equals(legacy.getEggTracker().getCurrentBearer())) {
                legacy.getPassiveEffectsEngine().removeFromPlayer(player);
                // Transition to OFFLINE_PLAYER so the heartbeat does not treat the
                // egg as missing and fire false "egg dropped" / fallback spawn logic.
                legacy.getEggTracker().updateEggOfflinePlayer(player.getUUID());
            }
            // Request immediate scrub to update egg state after player inventory is gone
            legacy.getEggCore().requestImmediateScrub();
        });
    }

    // -------------------------------------------------------------------------
    // World / block hooks
    // -------------------------------------------------------------------------

    private static void registerWorldHooks() {
        // Track when a dragon egg block is broken, and attempt promotion next tick
        // in case the broken egg lands in the breaking player's inventory.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!state.is(Blocks.DRAGON_EGG)) return;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;
            // Notify EggCore that the block was broken; state will update on next tick scan
            legacy.getEggCore().onEggBroken(pos, world);
            // Schedule a next-tick promotion attempt for the breaking player:
            // the broken egg item entity will typically be picked up immediately.
            if (player instanceof ServerPlayer sp) {
                MinecraftServer srv = world.getServer();
                srv.schedule(new TickTask(srv.getTickCount() + 1, () -> {
                    DragonsLegacy l = DragonsLegacy.getInstance();
                    if (l == null || l.getPersistentState().getCanonicalEggId() != null) return;
                    EggPromotion.tryPromote(sp);
                    l.getEggCore().requestImmediateScrub();
                }));
            }
        });

        // When a vanilla dragon egg item entity disappears from the world
        // (e.g. picked up by a player), schedule a next-tick promotion scan across
        // all online players so the egg is promoted before the scrubber runs.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof ItemEntity item)) return;
            if (!EggPromotion.isVanillaDragonEgg(item.getItem())) return;
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null || legacy.getPersistentState().getCanonicalEggId() != null) return;
            // The item entity is leaving the world – it may have been picked up by a player.
            // Schedule a next-tick scan so the player's inventory is checked after pickup.
            MinecraftServer srv = world.getServer();
            srv.schedule(new TickTask(srv.getTickCount() + 1, () -> {
                DragonsLegacy l = DragonsLegacy.getInstance();
                if (l == null || l.getPersistentState().getCanonicalEggId() != null) return;
                for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                    if (EggPromotion.tryPromote(p)) break;
                }
                l.getEggCore().requestImmediateScrub();
            }));
        });

        // Detect Ender Dragon death: mark the egg as initialized
        ServerEntityEvents.ENTITY_UNLOAD.register(
            (entity, world) -> {
                if (!(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) return;
                if (entity.getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.KILLED) return;
                DragonsLegacy legacy = DragonsLegacy.getInstance();
                if (legacy == null) return;
                legacy.getPersistentState().setEggInitialized(true);
            }
        );

        // Watch item entities that are dying and protect canonical egg
        ServerEntityEvents.ENTITY_UNLOAD.register(
            (entity, world) -> {
                if (!(entity instanceof ItemEntity item)) return;
                if (!EggCore.isDragonEgg(item.getItem())) return;
                if (!item.isRemoved()) return;

                DragonsLegacy legacy = DragonsLegacy.getInstance();
                if (legacy == null) return;

                EggProtectionManager protection = legacy.getEggProtectionManager();
                protection.onEggItemEntityAboutToDie(item);
            }
        );
    }

    // -------------------------------------------------------------------------
    // Ability hooks
    // -------------------------------------------------------------------------

    private static void registerAbilityHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DragonsLegacy legacy = DragonsLegacy.getInstance();
            if (legacy == null) return;

            legacy.getEventBus().subscribe(EggBearerChangedEvent.class, event -> {
                DragonsLegacy current = DragonsLegacy.getInstance();
                if (current == null) return;

                AbilityEngine engine = current.getAbilityEngine();

                // Deactivate ability and remove passive effects from old bearer
                if (event.getPreviousBearerUUID() != null) {
                    ServerPlayer oldBearer = server.getPlayerList().getPlayer(event.getPreviousBearerUUID());
                    if (oldBearer != null) {
                        engine.deactivateDragonHunger(oldBearer, "lost_bearer_status");
                        current.getPassiveEffectsEngine().removeFromPlayer(oldBearer);
                    }
                }

                // Apply passive effects to the new bearer; reset cooldown
                if (event.getNewBearerUUID() != null) {
                    engine.resetCooldownIfNeeded();
                    ServerPlayer newBearer = server.getPlayerList().getPlayer(event.getNewBearerUUID());
                    if (newBearer != null) {
                        current.getPassiveEffectsEngine().applyToBearer(newBearer);
                    }
                }
            });
        });
    }
}
