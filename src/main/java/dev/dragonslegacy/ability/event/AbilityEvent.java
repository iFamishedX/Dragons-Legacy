package dev.dragonslegacy.ability.event;

import dev.dragonslegacy.egg.event.DragonEggEvent;

/**
 * Marker interface for all Dragon's Legacy ability events.
 *
 * <p>Extends {@link DragonEggEvent} so that ability events can be published on the
 * shared {@link dev.dragonslegacy.egg.event.DragonEggEventBus} alongside egg-domain events,
 * allowing any subscriber to react to ability lifecycle changes.
 */
public interface AbilityEvent extends DragonEggEvent {}
