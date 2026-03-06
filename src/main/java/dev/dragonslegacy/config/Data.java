package dev.dragonslegacy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

import static dev.dragonslegacy.DragonsLegacyMod.*;

public class Data {
    public transient @Nullable Level world;
    @SerializedName("world")
    public @NotNull String worldId = "minecraft:overworld";
    @SerializedName("entity_uuid")
    public @Nullable UUID entityUUID;
    @SerializedName("player_uuid")
    public @Nullable UUID playerUUID;
    @SerializedName("last_change")
    public long lastChange = 0;
    public Durations durations = new Durations();
    public @Nullable DragonEggAPI.PositionType type;
    @SerializedName("position")
    private @Nullable Vector3f _position;
    @SerializedName("randomized_position")
    private @Nullable Vector3i _randomizedPosition;

    public static Data load() {
        Data data = new Data();
        File dataFile = CONFIG_DIR.resolve("data.json").toFile();
        try (Reader reader = new FileReader(dataFile)) {
            data = new Gson().fromJson(reader, Data.class);
        } catch (FileNotFoundException ignored) {
            LOGGER.debug("data.json not found, using default values");
        } catch (JsonIOException | IOException e) {
            LOGGER.warn("could not load saved data, using default values");
        } catch (JsonSyntaxException e) {
            LOGGER.warn("saved data is invalid: {}, using default values", e.getMessage());
        }
        return data;
    }

    public @NotNull BlockPos getRandomizedPosition() {
        if (this._randomizedPosition == null) {
            BlockPos randPos = Utils.randomizePosition(this.getBlockPos(), CONFIG.searchRadius);
            this._randomizedPosition = new Vector3i(randPos.getX(), randPos.getY(), randPos.getZ());
        }
        return new BlockPos(
            this._randomizedPosition.x,
            this._randomizedPosition.y,
            this._randomizedPosition.z
        );
    }

    public @NotNull BlockPos getBlockPos() {
        return BlockPos.containing(getPosition());
    }

    public @NotNull Vec3 getPosition() {
        return this._position == null ? Vec3.ZERO : new Vec3(this._position);
    }

    public void setPosition(@NotNull Vec3 position) {
        this._position = position.toVector3f();
    }

    public void clearRandomizedPosition() {
        this._randomizedPosition = null;
    }

    public long getBearerTime(long currentTime) {
        return durations.block + durations.player + durations.item + durations.entity + durations.inventory +
               durations.fallingBlock + getContinuousTime(currentTime);
    }

    public long getContinuousTime(long currentTime) {
        return currentTime - this.lastChange;
    }

    public long getDuration(DragonEggAPI.PositionType type) {
        return switch (type) {
            case BLOCK -> durations.block;
            case PLAYER -> durations.player;
            case ITEM -> durations.item;
            case ENTITY -> durations.entity;
            case INVENTORY -> durations.inventory;
            case FALLING_BLOCK -> durations.fallingBlock;
            case null -> 0;
        };
    }

    public void save() {
        File dataFile = CONFIG_DIR.resolve("data.json").toFile();
        try (Writer writer = new FileWriter(dataFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        } catch (JsonIOException | IOException e) {
            LOGGER.warn("could not save data: {}", e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Data data)) return false;
        return Objects.equals(worldId, data.worldId) &&
               Objects.equals(entityUUID, data.entityUUID) &&
               Objects.equals(playerUUID, data.playerUUID) &&
               Objects.equals(durations, data.durations) &&
               lastChange == data.lastChange &&
               type == data.type &&
               Objects.equals(_position, data._position) &&
               Objects.equals(_randomizedPosition, data._randomizedPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            worldId,
            entityUUID,
            playerUUID,
            durations,
            lastChange,
            type,
            _position,
            _randomizedPosition
        );
    }

    public static class Durations {
        public long block = 0;
        public long player = 0;
        public long item = 0;
        public long entity = 0;
        public long inventory = 0;
        @SerializedName("falling_block")
        public long fallingBlock = 0;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Durations that)) return false;
            return block == that.block &&
                   player == that.player &&
                   item == that.item &&
                   entity == that.entity &&
                   inventory == that.inventory &&
                   fallingBlock == that.fallingBlock;
        }

        @Override
        public int hashCode() {
            return Objects.hash(block, player, item, entity, inventory, fallingBlock);
        }
    }
}
