package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.LoanStatus;
import com.uniqueleaf.library.domain.Reservation;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.event.BookCheckedOutEvent;
import com.uniqueleaf.library.event.BookReturnedEvent;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.event.ReservedBookAvailableEvent;
import com.uniqueleaf.library.exception.CopyUnavailableException;
import com.uniqueleaf.library.exception.LoanNotFoundException;
import com.uniqueleaf.library.exception.PatronNotFoundException;
import com.uniqueleaf.library.policy.LoanPolicy;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.LoanRepository;
import com.uniqueleaf.library.repository.PatronRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class LendingService {
    private static final Logger logger = LoggerFactory.getLogger(LendingService.class);

    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final PatronRepository patronRepository;
    private final ReservationService reservationService;
    private final LoanPolicy loanPolicy;
    private final DomainEventPublisher eventPublisher;

    public LendingService(
            BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository,
            PatronRepository patronRepository,
            ReservationService reservationService,
            LoanPolicy loanPolicy,
            DomainEventPublisher eventPublisher) {
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.patronRepository = patronRepository;
        this.reservationService = reservationService;
        this.loanPolicy = loanPolicy;
        this.eventPublisher = eventPublisher;
    }

    public Loan checkoutCopy(PatronId patronId, CopyId copyId, LocalDate checkoutDate) {
        ensurePatronExists(patronId);

        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new CopyUnavailableException("Copy not found: " + copyId));

        if (loanRepository.findActiveByPatronId(patronId).stream().anyMatch(loan -> loan.copyId().equals(copyId))) {
            throw new CopyUnavailableException("Patron " + patronId + " already has an active loan for copy " + copyId);
        }
        if (loanRepository.findActiveByCopyId(copyId).isPresent()) {
            throw new CopyUnavailableException("Copy " + copyId + " is already checked out");
        }

        loanPolicy.ensureCanCheckout(loanRepository.findActiveByPatronId(patronId).size());
        Reservation notifiedReservation = null;

        if (copy.status() == CopyStatus.RESERVED_HOLD) {
            notifiedReservation = reservationService.findNotifiedReservationForCopy(copyId)
                    .orElseThrow(() -> new CopyUnavailableException("Copy " + copyId + " is on reserved hold"));
            if (!notifiedReservation.patronId().equals(patronId)) {
                throw new CopyUnavailableException("Copy " + copyId + " is reserved for another patron");
            }
            reservationService.fulfillReservation(notifiedReservation.reservationId(), checkoutDate.atStartOfDay());
        } else if (copy.status() != CopyStatus.AVAILABLE) {
            throw new CopyUnavailableException("Copy " + copyId + " is not available for checkout");
        }

        Loan loan = new Loan(
                LoanId.random(),
                copyId,
                patronId,
                checkoutDate,
                loanPolicy.calculateDueDate(checkoutDate),
                LoanStatus.ACTIVE,
                null);
        loanRepository.save(loan);
        BookCopy checkedOutCopy = copy.withStatus(CopyStatus.CHECKED_OUT);
        bookCopyRepository.save(checkedOutCopy);
        logger.info("Checkout success for copy {} by patron {}", copyId, patronId);
        eventPublisher.publish(new BookCheckedOutEvent(
                loan.loanId(),
                copy.copyId(),
                patronId,
                copy.isbn(),
                copy.branchId(),
                loan.dueDate()));
        return loan;
    }

    public Loan returnCopy(LoanId loanId, LocalDate returnDate) {
        Loan activeLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
        if (!activeLoan.isActive()) {
            throw new LoanNotFoundException("Loan " + loanId + " has already been returned");
        }

        BookCopy copy = bookCopyRepository.findById(activeLoan.copyId())
                .orElseThrow(() -> new CopyUnavailableException("Copy not found for loan " + loanId));

        Loan returnedLoan = activeLoan.markReturned(returnDate);
        loanRepository.save(returnedLoan);

        Optional<Reservation> nextReservation = reservationService.markNextReservationNotified(
                copy.isbn(),
                copy.branchId(),
                copy.copyId(),
                LocalDateTime.of(returnDate, java.time.LocalTime.NOON));

        boolean reservedHoldCreated = nextReservation.isPresent();
        BookCopy updatedCopy = reservedHoldCreated
                ? copy.withStatus(CopyStatus.RESERVED_HOLD)
                : copy.withStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(updatedCopy);

        logger.info("Return success for loan {} and copy {}", loanId, copy.copyId());
        eventPublisher.publish(new BookReturnedEvent(
                returnedLoan.loanId(),
                copy.copyId(),
                returnedLoan.patronId(),
                copy.isbn(),
                copy.branchId(),
                returnDate,
                reservedHoldCreated));
        nextReservation.ifPresent(reservation -> eventPublisher.publish(new ReservedBookAvailableEvent(
                reservation.reservationId(),
                copy.copyId(),
                copy.isbn(),
                reservation.patronId(),
                copy.branchId())));
        return returnedLoan;
    }

    private void ensurePatronExists(PatronId patronId) {
        if (!patronRepository.existsById(patronId)) {
            throw new PatronNotFoundException("Patron not found: " + patronId);
        }
    }
}
