package com.uniqueleaf.library;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public final class UniqueLeafDemo {
    private UniqueLeafDemo() {
    }

    public static void main(String[] args) {
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
        eventPublisher.register(
                com.uniqueleaf.library.event.ReservedBookAvailableEvent.class,
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

        Branch banyan = branchService.addBranch(new Branch(new BranchId("BR-BANYAN"), "Banyan Central Library", "1 Banyan Road"));
        Branch tamarind = branchService.addBranch(new Branch(new BranchId("BR-TAMARIND"), "Tamarind East Library", "21 Tamarind Lane"));

        Book ddd = catalogService.addBook(bookFactory.create(
                "Domain-Driven Design", "Eric Evans", "9780321125217", 2003, Set.of("design", "architecture")));
        Book refactoring = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("refactoring", "design")));

        var copyOne = catalogService.registerPhysicalCopy(ddd.isbn(), banyan.branchId());
        catalogService.registerPhysicalCopy(refactoring.isbn(), tamarind.branchId());

        Patron asha = patronService.registerPatron(patronFactory.create(
                "Asha Mehta", "asha@example.com", Set.of("design", "architecture"), banyan.branchId()));
        Patron kabir = patronService.registerPatron(patronFactory.create(
                "Kabir Sen", "kabir@example.com", Set.of("design"), banyan.branchId()));

        var loan = lendingService.checkoutCopy(asha.patronId(), copyOne.copyId(), LocalDate.now());
        reservationService.reserveBook(kabir.patronId(), ddd.isbn(), banyan.branchId(), LocalDateTime.now());
        lendingService.returnCopy(loan.loanId(), LocalDate.now().plusDays(7));

        System.out.println("Notifications sent: " + notificationChannel.sentMessages().size());
        for (Recommendation recommendation : recommendationService.recommendForPatron(asha.patronId())) {
            System.out.println(recommendation.book().title() + " -> " + recommendation.explanation());
        }
    }
}
