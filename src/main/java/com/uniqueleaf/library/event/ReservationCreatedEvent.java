package com.uniqueleaf.library.event;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;

import java.time.LocalDateTime;

public record ReservationCreatedEvent(
        ReservationId reservationId,
        Isbn isbn,
        PatronId patronId,
        BranchId branchId,
        LocalDateTime occurredAt) implements DomainEvent {

    public ReservationCreatedEvent(ReservationId reservationId, Isbn isbn, PatronId patronId, BranchId branchId) {
        this(reservationId, isbn, patronId, branchId, LocalDateTime.now());
    }
}
