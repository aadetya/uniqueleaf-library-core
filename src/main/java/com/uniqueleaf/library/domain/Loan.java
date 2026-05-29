package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;

import java.time.LocalDate;
import java.util.Objects;

public final class Loan {
    private final LoanId loanId;
    private final CopyId copyId;
    private final PatronId patronId;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private final LoanStatus status;
    private final LocalDate returnedDate;

    public Loan(
            LoanId loanId,
            CopyId copyId,
            PatronId patronId,
            LocalDate checkoutDate,
            LocalDate dueDate,
            LoanStatus status,
            LocalDate returnedDate) {
        this.loanId = Objects.requireNonNull(loanId, "loanId");
        this.copyId = Objects.requireNonNull(copyId, "copyId");
        this.patronId = Objects.requireNonNull(patronId, "patronId");
        this.checkoutDate = Objects.requireNonNull(checkoutDate, "checkoutDate");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
        this.status = Objects.requireNonNull(status, "status");
        if (dueDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Due date must not be before checkout date");
        }
        this.returnedDate = returnedDate;
    }

    public LoanId loanId() {
        return loanId;
    }

    public CopyId copyId() {
        return copyId;
    }

    public PatronId patronId() {
        return patronId;
    }

    public LocalDate checkoutDate() {
        return checkoutDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public LoanStatus status() {
        return status;
    }

    public LocalDate returnedDate() {
        return returnedDate;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    public Loan markReturned(LocalDate returnDate) {
        Objects.requireNonNull(returnDate, "returnDate");
        if (!isActive()) {
            throw new IllegalStateException("Loan " + loanId + " is already returned");
        }
        if (returnDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Return date must not be before checkout date");
        }
        return new Loan(loanId, copyId, patronId, checkoutDate, dueDate, LoanStatus.RETURNED, returnDate);
    }
}
