package dev.dragonslegacy.features;

import dev.dragonslegacy.Events;
import dev.dragonslegacy.api.APIUtils;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.api.Event;
import dev.dragonslegacy.api.EventsApi;
import dev.dragonslegacy.config.Action;
import dev.dragonslegacy.config.Condition.Variables;
import dev.dragonslegacy.config.Data;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.dragonslegacy.DragonsLegacyMod.*;

public class Actions {
    private static final Map<Identifier, HashSet<Consumer<Event<?>>>> registeredEventListeners = new HashMap<>();
    private static final Map<DragonEggAPI.PositionType, List<String>> positionTypeToTimeVariables = Map.of(
        DragonEggAPI.PositionType.BLOCK, List.of(
            Variables.CONTINUOUS_BLOCK_TIME,
            Variables.TOTAL_BLOCK_TIME
        ),
        DragonEggAPI.PositionType.PLAYER, List.of(
            Variables.CONTINUOUS_PLAYER_TIME,
            Variables.TOTAL_PLAYER_TIME
        ),
        DragonEggAPI.PositionType.ITEM, List.of(
            Variables.CONTINUOUS_ITEM_TIME,
            Variables.TOTAL_ITEM_TIME
        ),
        DragonEggAPI.PositionType.ENTITY, List.of(
            Variables.CONTINUOUS_ENTITY_TIME,
            Variables.TOTAL_ENTITY_TIME
        ),
        DragonEggAPI.PositionType.INVENTORY, List.of(
            Variables.CONTINUOUS_INVENTORY_TIME,
            Variables.TOTAL_INVENTORY_TIME
        ),
        DragonEggAPI.PositionType.FALLING_BLOCK, List.of(
            Variables.CONTINUOUS_FALLING_BLOCK_TIME,
            Variables.TOTAL_FALLING_BLOCK_TIME
        )
    );
    private static final Map<String, Supplier<String>> placeholders = Map.of(
        "bearer_id", () -> {
            Data data = DragonEggAPI.getData();
            return data != null && data.playerUUID != null ? data.playerUUID.toString() : "@a[predicate=deg:is_bearer]";
        },
        "bearer", () -> APIUtils.getBearer().getString(),
        "nearby", () -> "@a[predicate=deg:is_nearby]"
    );

    public static void register() {
        LOGGER.info("Registering event listeners for {}", MOD_ID);
        for (Map.Entry<Identifier, HashSet<Consumer<Event<?>>>> entry : registeredEventListeners.entrySet()) {
            for (Consumer<Event<?>> listener : entry.getValue()) {
                EventsApi.removeListener(entry.getKey(), listener);
            }
        }
        registeredEventListeners.clear();

        for (Action action : CONFIG.actions) {
            if (action.trigger() == null) {
                LOGGER.warn("Skipping event listener without trigger: {}", action);
                continue;
            }
            registeredEventListeners
                .computeIfAbsent(Identifier.parse(action.trigger()), k -> new HashSet<>())
                .add(event -> {
                    if (server == null) return;
                    action.executeSafe(
                        server.createCommandSourceStack().withSuppressedOutput(),
                        event.expressionVariables,
                        event.localPlaceholders
                    );
                });
        }

        for (Map.Entry<Identifier, HashSet<Consumer<Event<?>>>> entry : registeredEventListeners.entrySet()) {
            for (Consumer<Event<?>> listener : entry.getValue()) {
                EventsApi.listen(entry.getKey(), listener);
            }
        }
    }

    public static void init() {
        register();
        DragonEggAPI.onUpdate(Actions::onUpdate);
    }

    private static void onUpdate(Data data) {
        if (data.type == null) {
            Events.TICK_ACTIONS.remove("actions_tick");
            return;
        }

        Map<String, Double> variables = new HashMap<>(Map.of(
            Variables.X, data.getPosition().x,
            Variables.Y, data.getPosition().y,
            Variables.Z, data.getPosition().z,
            Variables.RAND_X, (double) data.getRandomizedPosition().getX(),
            Variables.RAND_Y, (double) data.getRandomizedPosition().getY(),
            Variables.RAND_Z, (double) data.getRandomizedPosition().getZ()
        ));

        Event<Void> event = new Event<>(variables, placeholders, null);

        Consumer<MinecraftServer> calculateVariables = server -> {
            long currentTime = server.overworld().getGameTime();
            variables.put(Variables.BEARER_TIME, Math.floor(data.getBearerTime(currentTime) / 20d));
            for (DragonEggAPI.PositionType type : DragonEggAPI.PositionType.values()) {
                List<String> value = positionTypeToTimeVariables.get(type);
                if (type == data.type) {
                    long continuousTime = data.getContinuousTime(currentTime);
                    variables.put(value.getFirst(), Math.floor(continuousTime / 20d));
                    variables.put(value.get(1), Math.floor((data.getDuration(type) + continuousTime) / 20d));
                    continue;
                }
                variables.put(value.getFirst(), 0d);
                variables.put(value.get(1), Math.floor(data.getDuration(type) / 20d));
            }
        };

        if (data.world != null) calculateVariables.accept(data.world.getServer());
        emitEvent(data.type.name().toLowerCase(), event);

        Events.TICK_ACTIONS.put(
            "actions_tick", (ticks, server) -> {
                if (ticks % 20 != 0) return;
                calculateVariables.accept(server);
                emitEvent("second", event);
            }
        );
    }

    private static void emitEvent(String eventName, Event<?> event) {
        try {
            EventsApi.emit(Identifier.fromNamespaceAndPath(MOD_ID_ALIAS, eventName), event);
        } catch (Exception e) {
            LOGGER.warn("Error during event handling: {}", e.getMessage());
        }
    }
}
