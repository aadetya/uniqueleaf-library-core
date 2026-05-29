package com.uniqueleaf.library.event;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookCheckedOutEvent(
        LoanId loanId,
        CopyId copyId,
        PatronId patronId,
        Isbn isbn,
        BranchId branchId,
        LocalDate dueDate,
        LocalDateTime occurredAt) implements DomainEvent {

    public BookCheckedOutEvent(
            LoanId loanId,
            CopyId copyId,
            PatronId patronId,
            Isbn isbn,
            BranchId branchId,
            LocalDate dueDate) {
        this(loanId, copyId, patronId, isbn, branchId, dueDate, LocalDateTime.now());
    }
}
