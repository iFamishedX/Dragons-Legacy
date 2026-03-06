package dev.dragonslegacy.config;

import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static dev.dragonslegacy.DragonsLegacyMod.LOGGER;
import static dev.dragonslegacy.DragonsLegacyMod.devLogger;

@ConfigSerializable
public record Action(
    @Nullable String trigger,
    @Nullable Condition condition,
    @Nullable List<Action> actions,
    @Nullable CommandTemplate command
) {
    public Action(@NotNull String commandTemplate) {
        this(null, null, null, new CommandTemplate(commandTemplate));
    }

    public Action(@NotNull String commandTemplate, @Nullable String condition) {
        this(null, condition != null ? new Condition(condition) : null, null, new CommandTemplate(commandTemplate));
    }

    public boolean checkCondition(@NotNull Map<String, Double> expressionVariables) {
        return condition == null || condition.evaluate(expressionVariables) != 0;
    }

    public void executeSafe(
        @NotNull CommandSourceStack commandSource,
        @NotNull Map<String, Double> expressionVariables,
        @NotNull Map<String, Supplier<String>> localPlaceholders
    ) {
        if (!checkCondition(expressionVariables)) return;
        try {
            execute(commandSource, expressionVariables, localPlaceholders);
        } catch (Exception e) {
            LOGGER.info("Error invoking action: {}", e.getMessage());
        }
    }

    public void execute(
        @NotNull CommandSourceStack commandSource,
        @NotNull Map<String, Double> expressionVariables,
        @NotNull Map<String, Supplier<String>> localPlaceholders
    ) {
        if (command != null) {
            String c = command.getCommand(expressionVariables, localPlaceholders);
            devLogger("Executing command: {}", c);
            commandSource.getServer().getCommands().performPrefixedCommand(commandSource, c);
        }
        if (actions != null)
            for (Action action : actions)
                action.executeSafe(commandSource, expressionVariables, localPlaceholders);
    }

    public static class Serializer implements TypeSerializer<Action> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public Action deserialize(@NotNull Type type, ConfigurationNode node) throws SerializationException {
            String command = node.getString();
            if (command != null) {
                return new Action(command);
            }

            String trigger = node.node("trigger").getString();
            Condition condition = node.node("condition").get(Condition.class);
            List<Action> actions = node.node("actions").getList(Action.class);
            CommandTemplate commandTemplate = node.node("command").get(CommandTemplate.class);
            return new Action(trigger, condition, actions, commandTemplate);
        }

        @Override
        public void serialize(
            @NotNull Type type,
            @Nullable Action obj,
            @NotNull ConfigurationNode node
        ) throws SerializationException {
            if (obj == null) {
                node.raw(null);
                return;
            }

            if (
                obj.trigger == null
                && obj.condition == null
                && obj.actions == null
                && obj.command != null
            ) {
                node.set(obj.command.value);
                return;
            }

            if (obj.trigger != null) {
                node.node("trigger").set(obj.trigger);
            }
            if (obj.condition != null && !obj.condition.isEmpty()) {
                node.node("condition").set(obj.condition);
            }
            if (obj.actions != null) {
                node.node("actions").setList(Action.class, obj.actions);
            }
            if (obj.command != null) {
                node.node("command").set(obj.command.value);
            }
        }
    }
}
