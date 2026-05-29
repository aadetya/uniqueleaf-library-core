package com.uniqueleaf.library;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;
import com.uniqueleaf.library.domain.ReservationStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.event.ReservedBookAvailableEvent;
import com.uniqueleaf.library.exception.CopyUnavailableException;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.factory.PatronFactory;
import com.uniqueleaf.library.notification.InMemoryNotificationChannel;
import com.uniqueleaf.library.notification.ReservationNotificationListener;
import com.uniqueleaf.library.policy.StandardLoanPolicy;
import com.uniqueleaf.library.recommendation.GenreAffinityRecommendationStrategy;
import com.uniqueleaf.library.repository.memory.InMemoryBookCopyRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBookRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBranchRepository;
import com.uniqueleaf.library.repository.memory.InMemoryLoanRepository;
import com.uniqueleaf.library.repository.memory.InMemoryPatronRepository;
import com.uniqueleaf.library.repository.memory.InMemoryReservationRepository;
import com.uniqueleaf.library.service.BranchService;
import com.uniqueleaf.library.service.CatalogService;
import com.uniqueleaf.library.service.LendingService;
import com.uniqueleaf.library.service.PatronService;
import com.uniqueleaf.library.service.RecommendationService;
import com.uniqueleaf.library.service.ReservationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniqueLeafIntegrationTest {
    @Test
    void runsTheUniqueLeafWorkflowAcrossBranchesReservationsAndRecommendations() {
        BookFactory bookFactory = new BookFactory();
        PatronFactory patronFactory = new PatronFactory();

        InMemoryBookRepository bookRepository = new InMemoryBookRepository();
        InMemoryBookCopyRepository bookCopyRepository = new InMemoryBookCopyRepository();
        InMemoryBranchRepository branchRepository = new InMemoryBranchRepository();
        InMemoryPatronRepository patronRepository = new InMemoryPatronRepository();
        InMemoryLoanRepository loanRepository = new InMemoryLoanRepository();
        InMemoryReservationRepository reservationRepository = new InMemoryReservationRepository();
        DomainEventPublisher eventPublisher = new DomainEventPublisher();
        InMemoryNotificationChannel notificationChannel = new InMemoryNotificationChannel();
        eventPublisher.register(ReservedBookAvailableEvent.class,
                new ReservationNotificationListener(notificationChannel, patronRepository, bookRepository));

        CatalogService catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        BranchService branchService = new BranchService(branchRepository, bookCopyRepository, eventPublisher);
        PatronService patronService = new PatronService(
                patronRepository, loanRepository, bookCopyRepository, bookRepository, patronFactory);
        ReservationService reservationService = new ReservationService(
                reservationRepository, bookCopyRepository, bookRepository, patronRepository, branchRepository, eventPublisher);
        LendingService lendingService = new LendingService(
                bookCopyRepository, loanRepository, patronRepository, reservationService, new StandardLoanPolicy(), eventPublisher);
        RecommendationService recommendationService = new RecommendationService(
                patronRepository, bookRepository, bookCopyRepository, loanRepository, new GenreAffinityRecommendationStrategy());

        BranchId banyanId = new BranchId("BR-BANYAN");
        BranchId tamarindId = new BranchId("BR-TAMARIND");
        Branch banyan = branchService.addBranch(new Branch(banyanId, "Banyan Central Library", "1 Banyan Road"));
        Branch tamarind = branchService.addBranch(new Branch(tamarindId, "Tamarind East Library", "2 Tamarind Lane"));

        Book pragmatic = catalogService.addBook(bookFactory.create(
                "The Pragmatic Programmer", "Andrew Hunt", "9780201616224", 1999, Set.of("software", "craft")));
        Book cleanCode = catalogService.addBook(bookFactory.create(
                "Clean Code", "Robert C. Martin", "9780132350884", 2008, Set.of("software", "craft")));
        Book designPatterns = catalogService.addBook(bookFactory.create(
                "Design Patterns", "Erich Gamma", "9780201633610", 1994, Set.of("design", "architecture")));
        Book refactoring = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("design")));
        Book ddd = catalogService.addBook(bookFactory.create(
                "Domain-Driven Design", "Eric Evans", "9780321125217", 2003, Set.of("architecture", "design")));

        var pragmaticCopy = catalogService.registerPhysicalCopy(pragmatic.isbn(), banyan.branchId());
        var cleanCodeCopy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan.branchId());
        var designPatternsCopy = catalogService.registerPhysicalCopy(designPatterns.isbn(), tamarind.branchId());
        var refactoringCopy = catalogService.registerPhysicalCopy(refactoring.isbn(), tamarind.branchId());
        catalogService.registerPhysicalCopy(ddd.isbn(), banyan.branchId());

        Patron asha = patronService.registerPatron(patronFactory.create(
                "Asha Mehta", "asha@example.com", Set.of("software", "design"), banyan.branchId()));
        Patron kabir = patronService.registerPatron(patronFactory.create(
                "Kabir Sen", "kabir@example.com", Set.of("software"), banyan.branchId()));
        Patron mira = patronService.registerPatron(patronFactory.create(
                "Mira Rao", "mira@example.com", Set.of("architecture", "design"), banyan.branchId()));

        var ashaLoan = lendingService.checkoutCopy(asha.patronId(), pragmaticCopy.copyId(), LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> lendingService.checkoutCopy(kabir.patronId(), pragmaticCopy.copyId(), LocalDate.of(2026, 5, 2)))
                .isInstanceOf(CopyUnavailableException.class);

        var reservation = reservationService.reserveBook(
                kabir.patronId(), pragmatic.isbn(), banyan.branchId(), LocalDateTime.of(2026, 5, 2, 11, 0));

        lendingService.returnCopy(ashaLoan.loanId(), LocalDate.of(2026, 5, 7));

        assertThat(notificationChannel.sentMessages()).hasSize(1);
        assertThat(notificationChannel.sentMessages().get(0).recipientEmail()).isEqualTo("kabir@example.com");
        assertThat(reservationRepository.findById(reservation.reservationId()).orElseThrow().status())
                .isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(bookCopyRepository.findById(pragmaticCopy.copyId()).orElseThrow().status()).isEqualTo(CopyStatus.RESERVED_HOLD);

        var kabirLoan = lendingService.checkoutCopy(kabir.patronId(), pragmaticCopy.copyId(), LocalDate.of(2026, 5, 8));

        assertThat(kabirLoan.copyId()).isEqualTo(pragmaticCopy.copyId());
        assertThat(reservationRepository.findById(reservation.reservationId()).orElseThrow().status())
                .isEqualTo(ReservationStatus.FULFILLED);

        List<Recommendation> miraRecommendations = recommendationService.recommendForPatron(mira.patronId(), 3);

        assertThat(miraRecommendations).extracting(recommendation -> recommendation.book().title())
                .contains("Domain-Driven Design", "Design Patterns");

        branchService.transferCopy(refactoringCopy.copyId(), banyan.branchId());

        assertThat(branchService.inventoryForBranch(tamarind.branchId())).extracting(copy -> copy.copyId())
                .doesNotContain(refactoringCopy.copyId());
        assertThat(branchService.inventoryForBranch(banyan.branchId())).extracting(copy -> copy.copyId())
                .contains(refactoringCopy.copyId(), cleanCodeCopy.copyId(), pragmaticCopy.copyId());
    }
}
