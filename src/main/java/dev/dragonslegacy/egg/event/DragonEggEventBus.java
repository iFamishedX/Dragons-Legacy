package dev.dragonslegacy.egg.event;

import de.arvitus.dragonegggame.DragonEggGame;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Simple synchronous publish–subscribe event bus for Dragon's Legacy egg events.
 *
 * <p>Subscribers register a {@link Consumer} typed to a specific {@link DragonEggEvent}
 * subtype; only events that are assignment-compatible with that type will be
 * delivered to the subscriber.
 */
public class DragonEggEventBus {

    private final Map<Class<?>, Set<Consumer<? extends DragonEggEvent>>> listeners =
        new ConcurrentHashMap<>();

    /**
     * Subscribes {@code listener} to events of type {@code T}.
     *
     * @param eventType  the exact event class to listen for
     * @param listener   the callback to invoke
     * @param <T>        the event type
     */
    public <T extends DragonEggEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners
            .computeIfAbsent(eventType, k -> new LinkedHashSet<>())
            .add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param eventType the event class the listener was registered for
     * @param listener  the callback to remove
     * @param <T>       the event type
     */
    public <T extends DragonEggEvent> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        Set<Consumer<? extends DragonEggEvent>> set = listeners.get(eventType);
        if (set != null) set.remove(listener);
    }

    /**
     * Publishes {@code event} to all registered subscribers whose event type is
     * assignable from the runtime type of {@code event}.
     *
     * @param event the event to publish (must not be {@code null})
     */
    @SuppressWarnings("unchecked")
    public <T extends DragonEggEvent> void publish(T event) {
        for (Map.Entry<Class<?>, Set<Consumer<? extends DragonEggEvent>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(event.getClass())) {
                for (Consumer<? extends DragonEggEvent> consumer : Set.copyOf(entry.getValue())) {
                    try {
                        ((Consumer<T>) consumer).accept(event);
                    } catch (Exception e) {
                        DragonEggGame.LOGGER.warn(
                            "[Dragon's Legacy] Exception in event listener for {}: {}",
                            event.getClass().getSimpleName(), e.getMessage(), e
                        );
                    }
                }
            }
        }
    }

    /** Removes all registered listeners (useful for clean shutdown / testing). */
    public void clearListeners() {
        listeners.clear();
    }
}
