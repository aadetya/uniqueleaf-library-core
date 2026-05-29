package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.LoanStatus;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.exception.CopyUnavailableException;
import com.uniqueleaf.library.exception.LoanLimitExceededException;
import com.uniqueleaf.library.exception.LoanNotFoundException;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.factory.PatronFactory;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LendingServiceTest {
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
    private PatronService patronService;
    private ReservationService reservationService;
    private LendingService lendingService;
    private BranchId banyan;
    private Patron asha;
    private Book cleanCode;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        patronService = new PatronService(patronRepository, loanRepository, bookCopyRepository, bookRepository, patronFactory);
        reservationService = new ReservationService(
                reservationRepository, bookCopyRepository, bookRepository, patronRepository, branchRepository, eventPublisher);
        lendingService = new LendingService(
                bookCopyRepository, loanRepository, patronRepository, reservationService, new StandardLoanPolicy(2, 14), eventPublisher);

        banyan = new BranchId("BR-BANYAN");
        branchRepository.save(new Branch(banyan, "Banyan Central Library", "1 Banyan Road"));
        cleanCode = catalogService.addBook(bookFactory.create(
                "Clean Code", "Robert C. Martin", "9780132350884", 2008, Set.of("software", "craft")));
        asha = patronService.registerPatron(patronFactory.create(
                "Asha Mehta", "asha@example.com", Set.of("software"), banyan));
    }

    @Test
    void checksOutAvailableCopy() {
        var copy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);

        Loan loan = lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));

        assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(bookCopyRepository.findById(copy.copyId()).orElseThrow().status()).isEqualTo(com.uniqueleaf.library.domain.CopyStatus.CHECKED_OUT);
    }

    @Test
    void preventsCheckoutOfUnavailableCopy() {
        var copy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);
        lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));
        Patron kabir = patronService.registerPatron(patronFactory.create(
                "Kabir Sen", "kabir@example.com", Set.of("software"), banyan));

        assertThatThrownBy(() -> lendingService.checkoutCopy(kabir.patronId(), copy.copyId(), LocalDate.of(2026, 5, 2)))
                .isInstanceOf(CopyUnavailableException.class);
    }

    @Test
    void returnsBookAndUpdatesBorrowingHistory() {
        var copy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);
        Loan loan = lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));

        Loan returned = lendingService.returnCopy(loan.loanId(), LocalDate.of(2026, 5, 7));

        assertThat(returned.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(bookCopyRepository.findById(copy.copyId()).orElseThrow().status())
                .isEqualTo(com.uniqueleaf.library.domain.CopyStatus.AVAILABLE);
        assertThat(patronService.borrowingHistory(asha.patronId())).hasSize(1);
        assertThat(patronService.borrowingHistory(asha.patronId()).get(0).status()).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    void enforcesMaximumActiveLoans() {
        Book ddd = catalogService.addBook(bookFactory.create(
                "Domain-Driven Design", "Eric Evans", "9780321125217", 2003, Set.of("architecture")));
        Book refactoring = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("design")));
        var firstCopy = catalogService.registerPhysicalCopy(cleanCode.isbn(), banyan);
        var secondCopy = catalogService.registerPhysicalCopy(ddd.isbn(), banyan);
        var thirdCopy = catalogService.registerPhysicalCopy(refactoring.isbn(), banyan);

        lendingService.checkoutCopy(asha.patronId(), firstCopy.copyId(), LocalDate.of(2026, 5, 1));
        lendingService.checkoutCopy(asha.patronId(), secondCopy.copyId(), LocalDate.of(2026, 5, 2));

        assertThatThrownBy(() -> lendingService.checkoutCopy(asha.patronId(), thirdCopy.copyId(), LocalDate.of(2026, 5, 3)))
                .isInstanceOf(LoanLimitExceededException.class);
    }

    @Test
    void throwsMeaningfulExceptionForUnknownLoanReturn() {
        assertThatThrownBy(() -> lendingService.returnCopy(new com.uniqueleaf.library.domain.value.LoanId("LN-UNKNOWN"), LocalDate.now()))
                .isInstanceOf(LoanNotFoundException.class)
                .hasMessageContaining("Loan not found");
    }
}
