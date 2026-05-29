package com.uniqueleaf.library.event;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;

import java.time.LocalDateTime;

public record ReservedBookAvailableEvent(
        ReservationId reservationId,
        CopyId copyId,
        Isbn isbn,
        PatronId patronId,
        BranchId branchId,
        LocalDateTime occurredAt) implements DomainEvent {

    public ReservedBookAvailableEvent(
            ReservationId reservationId,
            CopyId copyId,
            Isbn isbn,
            PatronId patronId,
            BranchId branchId) {
        this(reservationId, copyId, isbn, patronId, branchId, LocalDateTime.now());
    }
}
