package dev.dragonslegacy.egg;

import dev.dragonslegacy.DragonsLegacyMod;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cleanup-only scrubber for the canonical Dragon Egg.
 *
 * <p>Every scrub pass scans locations in the following priority order:
 * online player inventories → item entities.
 * Each Dragon Egg stack is classified and action is taken:
 *
 * <ul>
 *   <li><b>Case A – canonical</b>: {@code dragonslegacy:egg = true} AND
 *       {@code dragonslegacy:egg_id == canonicalEggId} → keep exactly one.</li>
 *   <li><b>Case B – vanilla / untagged</b>: plain {@link Items#DRAGON_EGG} with no
 *       Dragon's Legacy components → delete (counterfeit) and log.</li>
 *   <li><b>Case C – wrong UUID</b>: {@code dragonslegacy:egg = true} OR
 *       {@code dragonslegacy:egg_id} present, but {@code egg_id != canonicalEggId}
 *       → delete (glitched/duped copy) and log.</li>
 *   <li><b>No canonical ID</b>: {@code canonicalEggId == null} →
 *       do nothing. Vanilla eggs are preserved. Promotion must be triggered by
 *       an event handler via {@link EggPromotion}; never by the scrubber.</li>
 * </ul>
 *
 * <p>After a completely clean pass (exactly one canonical found, all counterfeits
 * deleted) the egg's UUID is scrambled via {@link EggCore#maybeScramble(MinecraftServer)}.
 */
public class EggAntiDupeEngine {

    EggAntiDupeEngine() {}

    // =========================================================================
    // Public entry point
    // =========================================================================

    /**
     * Runs the full scrub pass: scan, classify, delete counterfeits,
     * report anomalies, and (after a clean pass) request UUID scrambling.
     */
    public void scanAndResolveDuplicates(MinecraftServer server) {
        DragonsLegacy legacy = DragonsLegacy.getInstance();
        if (legacy == null) return;

        EggPersistentState state = legacy.getPersistentState();
        UUID canonicalId = state.getCanonicalEggId();

        // ── Step 1: collect all Dragon Egg stacks from online players ──────────
        List<TaggedStack> playerStacks  = new ArrayList<>();
        List<ItemEntity>  entityEntries = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            collectFromPlayer(player, playerStacks);
        }

        // ── Step 2: collect from dropped item entities ─────────────────────────
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                border.getMinX(), Utils.WORLD_Y_MIN, border.getMinZ(),
                border.getMaxX(), Utils.WORLD_Y_MAX, border.getMaxZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                ItemStack stack = item.getItem();
                if (stack.is(Items.DRAGON_EGG)) {
                    entityEntries.add(item);
                }
            }
        }

        // ── Step 3: if no canonical ID, do not delete or promote ──────────────
        // Vanilla eggs are preserved intact. An event handler will call
        // EggPromotion.tryPromote() when a player-inventory egg is available.
        if (canonicalId == null) {
            return;
        }

        // ── Step 4: classify and delete ────────────────────────────────────────
        List<TaggedStack> canonical   = new ArrayList<>();
        List<TaggedStack> counterfeits = new ArrayList<>();  // cases B and C

        for (TaggedStack ts : playerStacks) {
            classify(ts.stack, canonicalId, canonical, counterfeits, "player inventory of " + ts.context);
        }
        for (ItemEntity item : entityEntries) {
            String context = "item entity at " + item.blockPosition().toShortString() + " in " + item.level().dimension().identifier();
            classifyEntity(item, canonicalId, canonical, counterfeits, context);
        }

        // ── Step 5: delete counterfeits ────────────────────────────────────────
        for (TaggedStack ts : counterfeits) {
            ts.stack.setCount(0);
        }
        // (entity counterfeits are discarded in classifyEntity)

        // ── Step 6: resolve multiple canonical instances ────────────────────────
        if (canonical.size() > 1) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Multiple canonical eggs detected ({}) – keeping first, deleting rest.",
                canonical.size()
            );
            for (int i = 1; i < canonical.size(); i++) {
                TaggedStack extra = canonical.get(i);
                DragonsLegacyMod.LOGGER.warn(
                    "[Dragon's Legacy]   Removing extra canonical egg from: {}",
                    extra.context
                );
                extra.stack.setCount(0);
            }
        }

        // ── Step 7: after a clean pass, request UUID scrambling ────────────────
        boolean cleanPass = counterfeits.isEmpty() && canonical.size() == 1;
        if (cleanPass) {
            EggCore eggCore = legacy.getEggCore();
            eggCore.maybeScramble(server);
        }
    }

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Classifies a single {@link ItemStack} that is a Dragon Egg, adding it
     * to the appropriate list.  Logs deletions for Cases B and C.
     * Handles the migration case for legacy eggs (EGG=true but no EGG_ID).
     */
    private void classify(
            ItemStack stack,
            @Nullable UUID canonicalId,
            List<TaggedStack> canonical,
            List<TaggedStack> counterfeits,
            String context) {

        boolean hasEggComponent = Boolean.TRUE.equals(stack.get(EggComponents.EGG));
        UUID    eggIdComponent  = stack.get(EggComponents.EGG_ID);

        if (canonicalId != null && hasEggComponent && canonicalId.equals(eggIdComponent)) {
            // Case A – canonical
            canonical.add(new TaggedStack(stack, context, null));
            return;
        }

        if (!hasEggComponent && eggIdComponent == null) {
            // Case B – vanilla / untagged
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Deleted vanilla dragon egg (counterfeit) from: {}",
                context
            );
            counterfeits.add(new TaggedStack(stack, context, null));
            return;
        }

        if (hasEggComponent && eggIdComponent == null && canonicalId != null) {
            // Migration case – egg tagged with EGG=true but no UUID (pre-UUID format).
            // Assign the canonical UUID so it becomes Case A from now on.
            stack.set(EggComponents.EGG_ID, canonicalId);
            DragonsLegacyMod.LOGGER.info(
                "[Dragon's Legacy] Migrated legacy dragon egg (assigned canonical UUID) from: {}",
                context
            );
            canonical.add(new TaggedStack(stack, context, null));
            return;
        }

        // Case C – tagged but wrong UUID (or UUID present but doesn't match)
        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Deleted glitched dragon egg (mismatched ID: {}) from: {}",
            eggIdComponent, context
        );
        counterfeits.add(new TaggedStack(stack, context, null));
    }

    /** Classifies a Dragon Egg item entity, discarding it if it is a counterfeit. */
    private void classifyEntity(
            ItemEntity item,
            @Nullable UUID canonicalId,
            List<TaggedStack> canonical,
            List<TaggedStack> counterfeits,
            String context) {

        ItemStack stack = item.getItem();
        boolean hasEggComponent = Boolean.TRUE.equals(stack.get(EggComponents.EGG));
        UUID    eggIdComponent  = stack.get(EggComponents.EGG_ID);

        if (canonicalId != null && hasEggComponent && canonicalId.equals(eggIdComponent)) {
            canonical.add(new TaggedStack(stack, context, item));
            return;
        }

        if (!hasEggComponent && eggIdComponent == null) {
            DragonsLegacyMod.LOGGER.warn(
                "[Dragon's Legacy] Deleted vanilla dragon egg entity (counterfeit) at: {}",
                context
            );
            item.discard();
            counterfeits.add(new TaggedStack(stack, context, item));
            return;
        }

        if (hasEggComponent && eggIdComponent == null && canonicalId != null) {
            // Migration case for item entities
            stack.set(EggComponents.EGG_ID, canonicalId);
            item.setItem(stack);
            DragonsLegacyMod.LOGGER.info(
                "[Dragon's Legacy] Migrated legacy dragon egg entity (assigned canonical UUID) at: {}",
                context
            );
            canonical.add(new TaggedStack(stack, context, item));
            return;
        }

        DragonsLegacyMod.LOGGER.warn(
            "[Dragon's Legacy] Deleted glitched dragon egg entity (mismatched ID: {}) at: {}",
            eggIdComponent, context
        );
        item.discard();
        counterfeits.add(new TaggedStack(stack, context, item));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Collects all Dragon Egg {@link ItemStack}s from a player's full inventory. */
    private void collectFromPlayer(ServerPlayer player, List<TaggedStack> result) {
        String name = player.getGameProfile().name();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(Items.DRAGON_EGG)) result.add(new TaggedStack(stack, name, null));
        }
        for (net.minecraft.world.entity.EquipmentSlot slot
                : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(Items.DRAGON_EGG)) result.add(new TaggedStack(stack, name, null));
        }
    }

    /**
     * Returns {@code true} if the stack is a plain vanilla Dragon Egg with no
     * Dragon's Legacy components. Delegates to {@link EggPromotion#isVanillaDragonEgg}.
     */
    private static boolean isVanillaDragonEgg(ItemStack stack) {
        return EggPromotion.isVanillaDragonEgg(stack);
    }

    /** Checks whether an item entity carries a vanilla Dragon Egg (no components). */
    static boolean isVanillaDragonEgg(ItemEntity item) {
        return EggPromotion.isVanillaDragonEgg(item.getItem());
    }

    // =========================================================================
    // Inner types
    // =========================================================================

    /** Lightweight holder for a stack, its context description, and optional entity. */
    private record TaggedStack(
        ItemStack  stack,
        String     context,
        @Nullable ItemEntity entity) {}
}
