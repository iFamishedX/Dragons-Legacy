package dev.dragonslegacy.api;

import dev.dragonslegacy.Events;
import dev.dragonslegacy.config.Config;
import dev.dragonslegacy.config.Data;
import dev.dragonslegacy.utils.ScheduledEvent;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static dev.dragonslegacy.DragonsLegacyMod.*;


public class DragonEggAPI {
    private static final LinkedHashSet<Consumer<@NotNull Data>> onUpdateConsumers = new LinkedHashSet<>();
    private static final List<DeferredUpdate> deferredUpdates = new ArrayList<>();
    private static @Nullable Data data;

    public static void init() {
        Data oldData = data;
        load_data();
        if (Objects.equals(oldData, data)) return;
        dispatchUpdate();
    }

    private static void load_data() {
        data = Data.load();
        if (server != null) {
            data.world = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(data.worldId)));
            if (data.world == null) {
                LOGGER.warn("Could not find world with id '{}'", data.worldId);
            }
        }
    }

    public static synchronized void onUpdate(Consumer<@NotNull Data> consumer) {
        onUpdateConsumers.add(consumer);
        if (data != null) consumer.accept(data);
    }

    public static synchronized boolean unregisterListener(Consumer<@NotNull Data> consumer) {
        return onUpdateConsumers.remove(consumer);
    }

    private static void dispatchUpdate() {
        if (data != null) {
            List<DeferredUpdate> updatesToProcess = new ArrayList<>(deferredUpdates);
            deferredUpdates.clear();
            for (DeferredUpdate update : updatesToProcess) {
                updatePosition(update.type, update.pos, update.world, update.entity);
            }

            for (Consumer<@NotNull Data> listener : new ArrayList<>(onUpdateConsumers)) {
                try {
                    listener.accept(data);
                } catch (Exception e) {
                    LOGGER.warn("Error while dispatching update to listener {}: {}", listener, e.getStackTrace());
                }
            }
        } else LOGGER.warn("API is not ready, data is missing");
    }

    public static void clearPosition() {
        data = new Data();
        data.save();
        dispatchUpdate();
    }

    public static void updatePosition(@NotNull Entity entity) {
        updatePosition(getPositionType(entity), entity.position(), entity.level(), entity);
    }

    public static void updatePosition(@NotNull BlockEntity blockEntity) {
        updatePosition(
            PositionType.INVENTORY,
            blockEntity.getBlockPos(),
            Objects.requireNonNull(blockEntity.getLevel())
        );
    }

    public static void updatePosition(@NotNull BlockPos pos, @NotNull Level world) {
        updatePosition(PositionType.BLOCK, pos, world);
    }

    public static void updatePosition(@NotNull PositionType type, @NotNull BlockPos pos, @NotNull Level world) {
        updatePosition(type, pos.getCenter(), world, null);
    }

    private static synchronized void updatePosition(
        @NotNull PositionType type,
        @NotNull Vec3 pos,
        @NotNull Level world,
        @Nullable Entity entity
    ) {
        if (data == null) {
            LOGGER.warn("API not ready, deferring position update");
            deferredUpdates.add(new DeferredUpdate(type, pos, world, entity));
            return;
        }

        if (entity != null) trackEntity(entity);
        if (type == data.type && pos.distanceTo(data.getPosition()) < 0.00001) return;

        devLogger(
            "Updating Dragon Egg position to type: {}, pos: {}, world: {}, entity: {}",
            type,
            BlockPos.containing(pos).toShortString(),
            world.dimension().identifier(),
            entity
        );

        if (type != data.type || (entity != null && !Objects.equals(data.entityUUID, entity.getUUID()))) {
            long currentTime = Objects.requireNonNull(world.getServer()).overworld().getGameTime();
            long deltaTime = currentTime - data.lastChange;
            switch (data.type) {
                case BLOCK -> data.durations.block += deltaTime;
                case PLAYER -> data.durations.player += deltaTime;
                case ITEM -> data.durations.item += deltaTime;
                case ENTITY -> data.durations.entity += deltaTime;
                case INVENTORY -> data.durations.inventory += deltaTime;
                case FALLING_BLOCK -> data.durations.fallingBlock += deltaTime;
                case null -> {}
            }
            data.lastChange = currentTime;
        }

        data.entityUUID = entity != null ? entity.getUUID() : null;
        if (entity instanceof ServerPlayer player && !Objects.equals(data.playerUUID, player.getUUID())) {
            data.durations = new Data.Durations();
            data.playerUUID = player.getUUID();
        }

        Level oldWorld = data.world != null ? data.world : world;
        if (
            !oldWorld.equals(world) ||
            !pos.closerThan(data.getRandomizedPosition().getCenter(), CONFIG.searchRadius)
        ) {
            data.clearRandomizedPosition();
        }

        data.type = type;
        data.world = world;
        data.worldId = world.dimension().identifier().toString();
        data.setPosition(pos);

        data.save();
        dispatchUpdate();
    }

    private static synchronized void trackEntity(Entity entity) {
        if (CONFIG.getVisibility(getPositionType(entity)) == Config.VisibilityType.EXACT) entity.setGlowingTag(true);
        Events.SCHEDULED_ACTIONS.put(
            entity.getUUID(), new ScheduledEvent(
                100,
                server -> Optional.ofNullable(DragonEggAPI.getData()).ifPresent(data -> {
                    if (entity.isRemoved()) return;
                    entity.setGlowingTag(false);
                    if (!Utils.hasDragonEgg(entity)) return;
                    DragonEggAPI.updatePosition(entity);
                })
            )
        );
    }

    public static @Nullable Data getData() {
        return data;
    }

    public static PositionType getPositionType(@NotNull Entity entity) {
        return switch (entity) {
            case ItemEntity ignored -> PositionType.ITEM;
            case FallingBlockEntity ignored -> PositionType.FALLING_BLOCK;
            case Player ignored -> PositionType.PLAYER;
            default -> PositionType.ENTITY;
        };
    }

    public enum PositionType {
        /** The Dragon Egg is a Block */
        BLOCK,
        /** The Dragon Egg is an Item Entity */
        ITEM,
        /** The Dragon Egg is a Falling Block Entity */
        FALLING_BLOCK,
        /** The Dragon Egg Item is part of a block inventory */
        INVENTORY,
        /** A non-player entity is carrying the Dragon Egg */
        ENTITY,
        /** A player is carrying the Dragon Egg */
        PLAYER,
    }

    private record DeferredUpdate(PositionType type, Vec3 pos, Level world, Entity entity) {}
}
