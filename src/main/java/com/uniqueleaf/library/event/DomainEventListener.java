package com.uniqueleaf.library.event;

public interface DomainEventListener<T extends DomainEvent> {
    void onEvent(T event);
}
