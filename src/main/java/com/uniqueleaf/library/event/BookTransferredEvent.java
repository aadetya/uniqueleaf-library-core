package com.uniqueleaf.library.event;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;

import java.time.LocalDateTime;

public record BookTransferredEvent(
        CopyId copyId,
        Isbn isbn,
        BranchId fromBranchId,
        BranchId toBranchId,
        LocalDateTime occurredAt) implements DomainEvent {

    public BookTransferredEvent(CopyId copyId, Isbn isbn, BranchId fromBranchId, BranchId toBranchId) {
        this(copyId, isbn, fromBranchId, toBranchId, LocalDateTime.now());
    }
}
