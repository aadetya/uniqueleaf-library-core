package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Reservation;
import com.uniqueleaf.library.domain.ReservationStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.event.ReservationCreatedEvent;
import com.uniqueleaf.library.exception.BookNotFoundException;
import com.uniqueleaf.library.exception.BranchNotFoundException;
import com.uniqueleaf.library.exception.PatronNotFoundException;
import com.uniqueleaf.library.exception.ReservationException;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.BookRepository;
import com.uniqueleaf.library.repository.BranchRepository;
import com.uniqueleaf.library.repository.PatronRepository;
import com.uniqueleaf.library.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final PatronRepository patronRepository;
    private final BranchRepository branchRepository;
    private final DomainEventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            PatronRepository patronRepository,
            BranchRepository branchRepository,
            DomainEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.patronRepository = patronRepository;
        this.branchRepository = branchRepository;
        this.eventPublisher = eventPublisher;
    }

    public Reservation reserveBook(PatronId patronId, Isbn isbn, BranchId preferredBranchId, LocalDateTime createdAt) {
        ensurePatronExists(patronId);
        ensureBookExists(isbn);
        ensureBranchExists(preferredBranchId);

        if (bookCopyRepository.findFirstByIsbnAndBranchIdAndStatus(isbn, preferredBranchId, CopyStatus.AVAILABLE).isPresent()) {
            throw new ReservationException(
                    "Book " + isbn + " is available at branch " + preferredBranchId + "; reservation not allowed");
        }

        Reservation reservation = new Reservation(
                ReservationId.random(),
                isbn,
                patronId,
                preferredBranchId,
                ReservationStatus.WAITING,
                createdAt,
                null,
                null,
                null);
        logger.info("Reservation created for ISBN {} at branch {} by patron {}", isbn, preferredBranchId, patronId);
        Reservation saved = reservationRepository.save(reservation);
        eventPublisher.publish(new ReservationCreatedEvent(saved.reservationId(), saved.isbn(), saved.patronId(), saved.branchId()));
        return saved;
    }

    public List<Reservation> reservationQueue(Isbn isbn, BranchId branchId) {
        return reservationRepository.findByIsbnAndBranchId(isbn, branchId);
    }

    public Optional<Reservation> markNextReservationNotified(
            Isbn isbn,
            BranchId branchId,
            CopyId copyId,
            LocalDateTime notifiedAt) {
        List<Reservation> waitingReservations = reservationRepository.findWaitingByIsbnAndBranchId(isbn, branchId);
        if (waitingReservations.isEmpty()) {
            return Optional.empty();
        }
        Reservation notified = waitingReservations.get(0).markNotified(copyId, notifiedAt);
        logger.info("Reservation {} marked notified for copy {}", notified.reservationId(), copyId);
        return Optional.of(reservationRepository.save(notified));
    }

    public Optional<Reservation> findNotifiedReservationForCopy(CopyId copyId) {
        return reservationRepository.findNotifiedByCopyId(copyId);
    }

    public Reservation fulfillReservation(ReservationId reservationId, LocalDateTime fulfilledAt) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException("Reservation not found: " + reservationId));
        if (reservation.status() != ReservationStatus.NOTIFIED) {
            throw new ReservationException("Reservation " + reservationId + " is not in NOTIFIED state");
        }
        Reservation fulfilled = reservation.markFulfilled(fulfilledAt);
        logger.info("Reservation {} fulfilled", reservationId);
        return reservationRepository.save(fulfilled);
    }

    public List<Reservation> reservationsForPatron(PatronId patronId) {
        ensurePatronExists(patronId);
        return reservationRepository.findByPatronId(patronId);
    }

    private void ensurePatronExists(PatronId patronId) {
        if (!patronRepository.existsById(patronId)) {
            throw new PatronNotFoundException("Patron not found: " + patronId);
        }
    }

    private void ensureBookExists(Isbn isbn) {
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException("Book not found: " + isbn);
        }
    }

    private void ensureBranchExists(BranchId branchId) {
        if (branchRepository.findById(branchId).isEmpty()) {
            throw new BranchNotFoundException("Branch not found: " + branchId);
        }
    }
}
