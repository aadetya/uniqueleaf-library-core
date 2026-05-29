package com.uniqueleaf.library.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DomainEventPublisher {
    private final Map<Class<?>, List<DomainEventListener<?>>> listeners = new ConcurrentHashMap<>();

    public <T extends DomainEvent> void register(Class<T> eventType, DomainEventListener<? super T> listener) {
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void publish(DomainEvent event) {
        listeners.forEach((eventType, registeredListeners) -> {
            if (eventType.isAssignableFrom(event.getClass())) {
                registeredListeners.forEach(listener -> notifyListener(listener, event));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void notifyListener(DomainEventListener<?> listener, DomainEvent event) {
        ((DomainEventListener<T>) listener).onEvent((T) event);
    }
}
