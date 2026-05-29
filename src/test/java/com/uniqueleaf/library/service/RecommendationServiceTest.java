package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.factory.PatronFactory;
import com.uniqueleaf.library.policy.StandardLoanPolicy;
import com.uniqueleaf.library.recommendation.AuthorAffinityRecommendationStrategy;
import com.uniqueleaf.library.recommendation.GenreAffinityRecommendationStrategy;
import com.uniqueleaf.library.repository.memory.InMemoryBookCopyRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBookRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBranchRepository;
import com.uniqueleaf.library.repository.memory.InMemoryLoanRepository;
import com.uniqueleaf.library.repository.memory.InMemoryPatronRepository;
import com.uniqueleaf.library.repository.memory.InMemoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {
    private final BookFactory bookFactory = new BookFactory();
    private final PatronFactory patronFactory = new PatronFactory();
    private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository bookCopyRepository = new InMemoryBookCopyRepository();
    private final InMemoryBranchRepository branchRepository = new InMemoryBranchRepository();
    private final InMemoryPatronRepository patronRepository = new InMemoryPatronRepository();
    private final InMemoryLoanRepository loanRepository = new InMemoryLoanRepository();
    private final InMemoryReservationRepository reservationRepository = new InMemoryReservationRepository();
    private final DomainEventPublisher eventPublisher = new DomainEventPublisher();

    private CatalogService catalogService;
    private ReservationService reservationService;
    private LendingService lendingService;
    private BranchId banyan;
    private Patron mira;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        reservationService = new ReservationService(
                reservationRepository, bookCopyRepository, bookRepository, patronRepository, branchRepository, eventPublisher);
        lendingService = new LendingService(
                bookCopyRepository, loanRepository, patronRepository, reservationService, new StandardLoanPolicy(), eventPublisher);

        banyan = new BranchId("BR-BANYAN");
        branchRepository.save(new Branch(banyan, "Banyan Central Library", "1 Banyan Road"));
        mira = patronFactory.create("Mira Rao", "mira@example.com", Set.of("design", "architecture"), banyan);
        patronRepository.save(mira);
    }

    @Test
    void genreAffinityRanksMatchingGenresAboveUnrelatedBooks() {
        Book designPatterns = catalogService.addBook(bookFactory.create(
                "Design Patterns", "Erich Gamma", "9780201633610", 1994, Set.of("design", "architecture")));
        Book poetry = catalogService.addBook(bookFactory.create(
                "Poetry of Leaves", "Nila Das", "9780306406157", 1980, Set.of("poetry")));
        catalogService.registerPhysicalCopy(designPatterns.isbn(), banyan);
        catalogService.registerPhysicalCopy(poetry.isbn(), banyan);

        RecommendationService service = new RecommendationService(
                patronRepository, bookRepository, bookCopyRepository, loanRepository, new GenreAffinityRecommendationStrategy());

        List<Recommendation> recommendations = service.recommendForPatron(mira.patronId(), 2);

        assertThat(recommendations.get(0).book().title()).isEqualTo("Design Patterns");
    }

    @Test
    void authorAffinityRecommendsAuthorsFromBorrowingHistory() {
        Book refactoring = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("design")));
        Book patterns = catalogService.addBook(bookFactory.create(
                "Patterns of Enterprise Application Architecture", "Martin Fowler", "9780321127426", 2002, Set.of("architecture")));
        Book cleanCode = catalogService.addBook(bookFactory.create(
                "Clean Code", "Robert C. Martin", "9780132350884", 2008, Set.of("software")));
        var borrowedCopy = catalogService.registerPhysicalCopy(refactoring.isbn(), banyan);
        catalogService.registerPhysicalCopy(patterns.isbn(), banyan);
        catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);
        var loan = lendingService.checkoutCopy(mira.patronId(), borrowedCopy.copyId(), LocalDate.of(2026, 5, 1));
        lendingService.returnCopy(loan.loanId(), LocalDate.of(2026, 5, 8));

        RecommendationService service = new RecommendationService(
                patronRepository, bookRepository, bookCopyRepository, loanRepository, new AuthorAffinityRecommendationStrategy());

        List<Recommendation> recommendations = service.recommendForPatron(mira.patronId(), 2);

        assertThat(recommendations.get(0).book().title()).isEqualTo("Patterns of Enterprise Application Architecture");
    }

    @Test
    void excludesAlreadyBorrowedBooksFromRecommendations() {
        Book cleanCode = catalogService.addBook(bookFactory.create(
                "Clean Code", "Robert C. Martin", "9780132350884", 2008, Set.of("software")));
        Book ddd = catalogService.addBook(bookFactory.create(
                "Domain-Driven Design", "Eric Evans", "9780321125217", 2003, Set.of("architecture")));
        var cleanCodeCopy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);
        catalogService.registerPhysicalCopy(ddd.isbn(), banyan);
        var loan = lendingService.checkoutCopy(mira.patronId(), cleanCodeCopy.copyId(), LocalDate.of(2026, 5, 1));
        lendingService.returnCopy(loan.loanId(), LocalDate.of(2026, 5, 4));

        RecommendationService service = new RecommendationService(
                patronRepository, bookRepository, bookCopyRepository, loanRepository, new GenreAffinityRecommendationStrategy());

        List<Recommendation> recommendations = service.recommendForPatron(mira.patronId(), 5);

        assertThat(recommendations).extracting(recommendation -> recommendation.book().title())
                .doesNotContain("Clean Code")
                .contains("Domain-Driven Design");
    }

    @Test
    void ranksUnavailableBooksLowerThanAvailablePreferredBranchMatches() {
        Book available = catalogService.addBook(bookFactory.create(
                "Practical Architecture", "Ira Cole", "9780134494166", 2017, Set.of("architecture")));
        Book unavailable = catalogService.addBook(bookFactory.create(
                "Hidden Architecture", "Lena Morse", "9780131177055", 2005, Set.of("architecture")));
        catalogService.registerPhysicalCopy(available.isbn(), banyan);

        RecommendationService service = new RecommendationService(
                patronRepository, bookRepository, bookCopyRepository, loanRepository, new GenreAffinityRecommendationStrategy());

        List<Recommendation> recommendations = service.recommendForPatron(mira.patronId(), 5);

        assertThat(recommendations.get(0).book().title()).isEqualTo("Practical Architecture");
        assertThat(recommendations).extracting(recommendation -> recommendation.book().title())
                .contains("Hidden Architecture");
    }
}
