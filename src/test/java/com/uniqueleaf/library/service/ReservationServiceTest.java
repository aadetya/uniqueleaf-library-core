package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Reservation;
import com.uniqueleaf.library.domain.ReservationStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.event.ReservedBookAvailableEvent;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.factory.PatronFactory;
import com.uniqueleaf.library.notification.InMemoryNotificationChannel;
import com.uniqueleaf.library.notification.ReservationNotificationListener;
import com.uniqueleaf.library.policy.StandardLoanPolicy;
import com.uniqueleaf.library.repository.memory.InMemoryBookCopyRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBookRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBranchRepository;
import com.uniqueleaf.library.repository.memory.InMemoryLoanRepository;
import com.uniqueleaf.library.repository.memory.InMemoryPatronRepository;
import com.uniqueleaf.library.repository.memory.InMemoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationServiceTest {
    private final BookFactory bookFactory = new BookFactory();
    private final PatronFactory patronFactory = new PatronFactory();
    private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository bookCopyRepository = new InMemoryBookCopyRepository();
    private final InMemoryBranchRepository branchRepository = new InMemoryBranchRepository();
    private final InMemoryPatronRepository patronRepository = new InMemoryPatronRepository();
    private final InMemoryLoanRepository loanRepository = new InMemoryLoanRepository();
    private final InMemoryReservationRepository reservationRepository = new InMemoryReservationRepository();
    private final DomainEventPublisher eventPublisher = new DomainEventPublisher();
    private final InMemoryNotificationChannel notificationChannel = new InMemoryNotificationChannel();

    private CatalogService catalogService;
    private PatronService patronService;
    private ReservationService reservationService;
    private LendingService lendingService;
    private BranchId banyan;
    private Patron asha;
    private Patron kabir;
    private Book pragmatic;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        patronService = new PatronService(patronRepository, loanRepository, bookCopyRepository, bookRepository, patronFactory);
        reservationService = new ReservationService(
                reservationRepository, bookCopyRepository, bookRepository, patronRepository, branchRepository, eventPublisher);
        lendingService = new LendingService(
                bookCopyRepository, loanRepository, patronRepository, reservationService, new StandardLoanPolicy(), eventPublisher);
        eventPublisher.register(ReservedBookAvailableEvent.class,
                new ReservationNotificationListener(notificationChannel, patronRepository, bookRepository));

        banyan = new BranchId("BR-BANYAN");
        branchRepository.save(new Branch(banyan, "Banyan Central Library", "1 Banyan Road"));
        pragmatic = catalogService.addBook(bookFactory.create(
                "The Pragmatic Programmer", "Andrew Hunt", "9780201616224", 1999, Set.of("software", "craft")));
        asha = patronService.registerPatron(patronFactory.create("Asha Mehta", "asha@example.com", Set.of("software"), banyan));
        kabir = patronService.registerPatron(patronFactory.create("Kabir Sen", "kabir@example.com", Set.of("software"), banyan));
    }

    @Test
    void reservesUnavailableBook() {
        Reservation reservation = reservationService.reserveBook(
                kabir.patronId(), pragmatic.isbn(), banyan, LocalDateTime.of(2026, 5, 1, 10, 0));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.WAITING);
        assertThat(reservationRepository.findByPatronId(kabir.patronId())).hasSize(1);
    }

    @Test
    void preservesFifoOrderingForReservationQueue() {
        Reservation first = reservationService.reserveBook(
                asha.patronId(), pragmatic.isbn(), banyan, LocalDateTime.of(2026, 5, 1, 10, 0));
        Reservation second = reservationService.reserveBook(
                kabir.patronId(), pragmatic.isbn(), banyan, LocalDateTime.of(2026, 5, 1, 11, 0));

        assertThat(reservationService.reservationQueue(pragmatic.isbn(), banyan))
                .extracting(Reservation::reservationId)
                .containsExactly(first.reservationId(), second.reservationId());
    }

    @Test
    void returningReservedBookMovesCopyToHoldAndSendsNotification() {
        var copy = catalogService.registerPhysicalCopy(pragmatic.isbn(), banyan);
        var loan = lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));
        reservationService.reserveBook(kabir.patronId(), pragmatic.isbn(), banyan, LocalDateTime.of(2026, 5, 2, 10, 0));

        lendingService.returnCopy(loan.loanId(), LocalDate.of(2026, 5, 5));

        Reservation notified = reservationRepository.findByPatronId(kabir.patronId()).get(0);
        assertThat(notified.status()).isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(bookCopyRepository.findById(copy.copyId()).orElseThrow().status()).isEqualTo(CopyStatus.RESERVED_HOLD);
        assertThat(notificationChannel.sentMessages()).hasSize(1);
        assertThat(notificationChannel.sentMessages().get(0).recipientEmail()).isEqualTo("kabir@example.com");
    }

    @Test
    void checkoutOfReservedHoldFulfillsReservation() {
        var copy = catalogService.registerPhysicalCopy(pragmatic.isbn(), banyan);
        var loan = lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));
        Reservation reservation = reservationService.reserveBook(
                kabir.patronId(), pragmatic.isbn(), banyan, LocalDateTime.of(2026, 5, 2, 10, 0));
        lendingService.returnCopy(loan.loanId(), LocalDate.of(2026, 5, 4));

        lendingService.checkoutCopy(kabir.patronId(), copy.copyId(), LocalDate.of(2026, 5, 6));

        Reservation fulfilled = reservationRepository.findById(reservation.reservationId()).orElseThrow();
        assertThat(fulfilled.status()).isEqualTo(ReservationStatus.FULFILLED);
    }
}
