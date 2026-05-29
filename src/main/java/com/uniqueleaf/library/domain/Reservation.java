package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public final class Reservation {
    private final ReservationId reservationId;
    private final Isbn isbn;
    private final PatronId patronId;
    private final BranchId branchId;
    private final ReservationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime notifiedAt;
    private final LocalDateTime fulfilledAt;
    private final CopyId assignedCopyId;

    public Reservation(
            ReservationId reservationId,
            Isbn isbn,
            PatronId patronId,
            BranchId branchId,
            ReservationStatus status,
            LocalDateTime createdAt,
            LocalDateTime notifiedAt,
            LocalDateTime fulfilledAt,
            CopyId assignedCopyId) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.isbn = Objects.requireNonNull(isbn, "isbn");
        this.patronId = Objects.requireNonNull(patronId, "patronId");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.notifiedAt = notifiedAt;
        this.fulfilledAt = fulfilledAt;
        this.assignedCopyId = assignedCopyId;
    }

    public ReservationId reservationId() {
        return reservationId;
    }

    public Isbn isbn() {
        return isbn;
    }

    public PatronId patronId() {
        return patronId;
    }

    public BranchId branchId() {
        return branchId;
    }

    public ReservationStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public Optional<LocalDateTime> notifiedAt() {
        return Optional.ofNullable(notifiedAt);
    }

    public Optional<LocalDateTime> fulfilledAt() {
        return Optional.ofNullable(fulfilledAt);
    }

    public Optional<CopyId> assignedCopyId() {
        return Optional.ofNullable(assignedCopyId);
    }

    public boolean isWaiting() {
        return status == ReservationStatus.WAITING;
    }

    public Reservation markNotified(CopyId copyId, LocalDateTime notifiedOn) {
        Objects.requireNonNull(copyId, "copyId");
        Objects.requireNonNull(notifiedOn, "notifiedOn");
        return new Reservation(
                reservationId,
                isbn,
                patronId,
                branchId,
                ReservationStatus.NOTIFIED,
                createdAt,
                notifiedOn,
                fulfilledAt,
                copyId);
    }

    public Reservation markFulfilled(LocalDateTime fulfilledOn) {
        Objects.requireNonNull(fulfilledOn, "fulfilledOn");
        return new Reservation(
                reservationId,
                isbn,
                patronId,
                branchId,
                ReservationStatus.FULFILLED,
                createdAt,
                notifiedAt,
                fulfilledOn,
                assignedCopyId);
    }

    public Reservation cancel() {
        return new Reservation(
                reservationId,
                isbn,
                patronId,
                branchId,
                ReservationStatus.CANCELLED,
                createdAt,
                notifiedAt,
                fulfilledAt,
                assignedCopyId);
    }
}
