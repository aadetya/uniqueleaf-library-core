package com.uniqueleaf.library.event;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookReturnedEvent(
        LoanId loanId,
        CopyId copyId,
        PatronId patronId,
        Isbn isbn,
        BranchId branchId,
        LocalDate returnDate,
        boolean reservedHoldCreated,
        LocalDateTime occurredAt) implements DomainEvent {

    public BookReturnedEvent(
            LoanId loanId,
            CopyId copyId,
            PatronId patronId,
            Isbn isbn,
            BranchId branchId,
            LocalDate returnDate,
            boolean reservedHoldCreated) {
        this(loanId, copyId, patronId, isbn, branchId, returnDate, reservedHoldCreated, LocalDateTime.now());
    }
}
