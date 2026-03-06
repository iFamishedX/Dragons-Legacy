package dev.dragonslegacy.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.dragonslegacy.DragonsLegacyMod.LOGGER;

public class CommandTemplate {
    public final String value;
    private final List<CommandTemplatePart> templateParts;

    public CommandTemplate(@NotNull String commandTemplate) {
        this.value = commandTemplate.strip();

        List<CommandTemplatePart> templateParts = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\$\\{([^{}]+)})|(\\{\\{(\\w+)}})");
        Matcher matcher = pattern.matcher(this.value);

        int lastEnd = 0;
        for (MatchResult match : matcher.results().toList()) {
            if (match.start() > lastEnd)
                templateParts.add(new CommandTemplatePart(this.value.substring(lastEnd, match.start()), null, null));
            if (match.group(1) != null)
                templateParts.add(new CommandTemplatePart(null, null, new Condition(match.group(2))));
            else if (match.group(3) != null)
                templateParts.add(new CommandTemplatePart(null, match.group(4), null));
            lastEnd = match.end();
        }
        templateParts.add(new CommandTemplatePart(this.value.substring(lastEnd), null, null));
        this.templateParts = templateParts.stream().toList();
    }

    public String getCommand(
        @NotNull Map<String, Double> expressionVariables,
        @NotNull Map<String, Supplier<String>> placeholders
    ) throws IllegalArgumentException {
        StringBuilder builder = new StringBuilder();
        boolean evaluationError = false;
        for (CommandTemplatePart templatePart : templateParts) {
            try {
                builder.append(templatePart.evaluate(expressionVariables, placeholders));
            } catch (Exception e) {
                if (templatePart.expression == null) throw e;

                evaluationError = true;
                LOGGER.warn(
                    "Error evaluating expression '{}': {}",
                    templatePart.expression.expressionString,
                    e.getMessage()
                );
            }
        }
        if (evaluationError) throw new IllegalArgumentException("Error evaluating one or more expressions");
        return builder.toString();
    }

    private record CommandTemplatePart(
        @Nullable String literal,
        @Nullable String placeholder,
        @Nullable Condition expression
    ) {
        private static final DecimalFormat decimalFormat = new DecimalFormat(
            "#.########",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH)
        );

        public CommandTemplatePart {
            int valueCount = 0;
            if (literal != null) valueCount++;
            if (placeholder != null) valueCount++;
            if (expression != null) valueCount++;
            if (valueCount < 1)
                throw new IllegalArgumentException("At least one of literal, placeholder, or expression must be set");
            if (valueCount > 1)
                throw new IllegalArgumentException("Only one of literal, placeholder, or expression can be set");
        }

        public String evaluate(
            @NotNull Map<String, Double> expressionVariables,
            @NotNull Map<String, Supplier<String>> placeholders
        ) {
            if (literal != null) return literal;
            if (placeholder != null) return placeholders
                .getOrDefault(placeholder, () -> "{{" + placeholder + "}}")
                .get();
            return decimalFormat.format(Objects.requireNonNull(expression).evaluate(expressionVariables));
        }
    }

    public static class Serializer implements TypeSerializer<CommandTemplate> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public CommandTemplate deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String value = node.getString();
            if (value == null) throw new SerializationException("Command template string is null");
            return new CommandTemplate(value);
        }

        @Override
        public void serialize(
            Type type,
            @Nullable CommandTemplate obj,
            ConfigurationNode node
        ) throws SerializationException {
            if (obj == null) node.raw(null);
            else node.set(obj.value);
        }
    }
}
