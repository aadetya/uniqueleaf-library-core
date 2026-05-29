package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.exception.LibraryException;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchServiceTest {
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
    private BranchService branchService;
    private LendingService lendingService;
    private ReservationService reservationService;
    private BranchId banyan;
    private BranchId tamarind;
    private Patron asha;
    private Book designPatterns;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        branchService = new BranchService(branchRepository, bookCopyRepository, eventPublisher);
        reservationService = new ReservationService(
                reservationRepository, bookCopyRepository, bookRepository, patronRepository, branchRepository, eventPublisher);
        lendingService = new LendingService(
                bookCopyRepository, loanRepository, patronRepository, reservationService, new StandardLoanPolicy(), eventPublisher);

        banyan = new BranchId("BR-BANYAN");
        tamarind = new BranchId("BR-TAMARIND");
        branchService.addBranch(new Branch(banyan, "Banyan Central Library", "1 Banyan Road"));
        branchService.addBranch(new Branch(tamarind, "Tamarind East Library", "2 Tamarind Lane"));
        designPatterns = catalogService.addBook(bookFactory.create(
                "Design Patterns", "Erich Gamma", "9780201633610", 1994, Set.of("design")));
        asha = new PatronFactory().create("Asha Mehta", "asha@example.com", Set.of("design"), banyan);
        patronRepository.save(asha);
    }

    @Test
    void transfersAvailableCopyBetweenBranches() {
        var copy = catalogService.registerPhysicalCopy(designPatterns.isbn(), banyan);

        var transferred = branchService.transferCopy(copy.copyId(), tamarind);

        assertThat(transferred.branchId()).isEqualTo(tamarind);
        assertThat(branchService.inventoryForBranch(banyan)).isEmpty();
        assertThat(branchService.inventoryForBranch(tamarind)).hasSize(1);
    }

    @Test
    void preventsTransferOfCheckedOutCopy() {
        var copy = catalogService.registerPhysicalCopy(designPatterns.isbn(), banyan);
        lendingService.checkoutCopy(asha.patronId(), copy.copyId(), LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> branchService.transferCopy(copy.copyId(), tamarind))
                .isInstanceOf(LibraryException.class)
                .hasMessageContaining("Checked-out copy");
    }

    @Test
    void inventoryCountsChangeAfterTransfer() {
        Book refactoring = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("design")));
        var copy = catalogService.registerPhysicalCopy(refactoring.isbn(), banyan);

        branchService.transferCopy(copy.copyId(), tamarind);

        assertThat(branchService.inventoryForBranch(banyan)).hasSize(0);
        assertThat(branchService.inventoryForBranch(tamarind)).hasSize(1);
    }
}
