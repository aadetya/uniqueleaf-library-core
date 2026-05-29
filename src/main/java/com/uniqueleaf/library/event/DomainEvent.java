package com.uniqueleaf.library.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    LocalDateTime occurredAt();
}
