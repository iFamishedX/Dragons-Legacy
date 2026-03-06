package dev.dragonslegacy.api;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class Event<T> {
    public static Event<Void> EMPTY = new Event<>(Map.of(), Map.of(), null);

    public final Map<String, Double> expressionVariables;
    public final Map<String, Supplier<String>> localPlaceholders;
    @Nullable
    public final T data;

    public Event(
        Map<String, Double> expressionVariables,
        Map<String, Supplier<String>> localPlaceholders,
        @Nullable T data
    ) {
        this.expressionVariables = expressionVariables;
        this.localPlaceholders = localPlaceholders;
        this.data = data;
    }

    public boolean hasData() {
        return data != null;
    }
}
