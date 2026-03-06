package dev.dragonslegacy.api;

import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

public abstract class EventsApi {
    private static final Map<Identifier, LinkedHashSet<Consumer<Event<?>>>> eventListeners = new HashMap<>();
    private static final Identifier ANY = Identifier.fromNamespaceAndPath("events", "any");

    public static void listen(Consumer<Event<?>> listener) {
        listen(ANY, listener);
    }

    public static synchronized void listen(Identifier eventId, Consumer<Event<?>> listener) {
        eventListeners.computeIfAbsent(eventId, k -> new LinkedHashSet<>()).add(listener);
    }

    public static void removeListener(Consumer<Event<?>> listener) {
        removeListener(ANY, listener);
    }

    public static synchronized void removeListener(Identifier eventId, Consumer<Event<?>> listener) {
        if (eventListeners.containsKey(eventId)) eventListeners.get(eventId).remove(listener);
    }

    public static boolean hasListeners(Identifier eventId) {
        return eventListeners.containsKey(eventId) && !eventListeners.get(eventId).isEmpty();
    }

    public static synchronized boolean emit(Identifier eventId, Event<?> event) throws Exception {
        List<Exception> thrownExceptions = new ArrayList<>(0);

        for (Consumer<Event<?>> listener : eventListeners.getOrDefault(eventId, new LinkedHashSet<>())) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                thrownExceptions.add(e);
            }
        }
        if (eventId != ANY)
            try {
                emit(ANY, event);
            } catch (Exception e) {
                thrownExceptions.add(e);
            }

        return throwAsOne(thrownExceptions);

    }

    private static boolean throwAsOne(List<Exception> exceptions) throws Exception {
        if (exceptions.isEmpty()) return true;

        Exception e = exceptions.getFirst();
        for (int i = 1; i < exceptions.size(); i++) {
            e.addSuppressed(exceptions.get(i));
        }
        throw e;
    }
}
