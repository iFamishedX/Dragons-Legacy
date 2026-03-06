package dev.dragonslegacy;

import com.mojang.serialization.MapCodec;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.config.Data;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import static dev.dragonslegacy.DragonsLegacyMod.CONFIG;
import static dev.dragonslegacy.DragonsLegacyMod.MOD_ID;

public class LootConditions {
    public static final LootItemConditionType IS_BEARER = register("is_bearer", IsBearer.CODEC);
    public static final LootItemConditionType IS_NEARBY = register("is_nearby", IsNearby.CODEC);

    public static void register() {}

    private static LootItemConditionType register(String id, MapCodec<? extends LootItemCondition> codec) {
        return Registry.register(
            BuiltInRegistries.LOOT_CONDITION_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, id),
            new LootItemConditionType(codec)
        );
    }

    public static class IsBearer implements LootItemCondition {
        public static final MapCodec<IsBearer> CODEC = MapCodec.unit(new IsBearer());

        @Override
        public LootItemConditionType getType() {
            return LootConditions.IS_BEARER;
        }

        @Override
        public boolean test(LootContext context) {
            Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
            Data data = DragonEggAPI.getData();
            if (entity instanceof ServerPlayer player && data != null)
                return player.getUUID().equals(data.playerUUID);
            return false;
        }
    }

    public static class IsNearby implements LootItemCondition {
        public static final MapCodec<IsNearby> CODEC = MapCodec.unit(new IsNearby());

        @Override
        public LootItemConditionType getType() {
            return LootConditions.IS_NEARBY;
        }

        @Override
        public boolean test(LootContext context) {
            Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
            Data data = DragonEggAPI.getData();
            if (entity != null && data != null && data.world != null)
                return entity.position().closerThan(data.getPosition(), CONFIG.nearbyRange);
            return false;
        }
    }
}


