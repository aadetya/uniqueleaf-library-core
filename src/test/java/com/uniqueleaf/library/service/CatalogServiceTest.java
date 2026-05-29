package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.repository.memory.InMemoryBookCopyRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBookRepository;
import com.uniqueleaf.library.repository.memory.InMemoryBranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogServiceTest {
    private final BookFactory bookFactory = new BookFactory();
    private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository bookCopyRepository = new InMemoryBookCopyRepository();
    private final InMemoryBranchRepository branchRepository = new InMemoryBranchRepository();

    private CatalogService catalogService;
    private BranchId banyan;
    private BranchId tamarind;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, bookCopyRepository, branchRepository, bookFactory);
        banyan = new BranchId("BR-BANYAN");
        tamarind = new BranchId("BR-TAMARIND");
        branchRepository.save(new Branch(banyan, "Banyan Central Library", "1 Banyan Road"));
        branchRepository.save(new Branch(tamarind, "Tamarind East Library", "2 Tamarind Lane"));
    }

    @Test
    void addsSearchesUpdatesAndRemovesBooks() {
        Book cleanCode = catalogService.addBook(bookFactory.create(
                "Clean Code", "Robert C. Martin", "9780132350884", 2008, Set.of("software", "craft")));
        catalogService.addBook(bookFactory.create(
                "Design Patterns", "Erich Gamma", "9780201633610", 1994, Set.of("design", "architecture")));

        assertThat(catalogService.searchByTitle("clean")).extracting(Book::title).containsExactly("Clean Code");
        assertThat(catalogService.searchByAuthor("gamma")).extracting(Book::title).containsExactly("Design Patterns");
        assertThat(catalogService.searchByIsbn(cleanCode.isbn()).author()).isEqualTo("Robert C. Martin");

        Book updated = catalogService.updateBook(
                cleanCode.isbn(),
                "Clean Code Second Shelf",
                "Robert C. Martin",
                2008,
                Set.of("software", "craft"));

        assertThat(updated.title()).isEqualTo("Clean Code Second Shelf");

        catalogService.removeBook(cleanCode.isbn());

        assertThat(bookRepository.existsByIsbn(cleanCode.isbn())).isFalse();
    }

    @Test
    void registersMultipleCopiesAcrossBranches() {
        Book book = catalogService.addBook(bookFactory.create(
                "Refactoring", "Martin Fowler", "9780201485677", 1999, Set.of("refactoring", "design")));

        BookCopy banyanCopy = catalogService.registerPhysicalCopy(book.isbn(), banyan);
        BookCopy tamarindCopy = catalogService.registerPhysicalCopy(book.isbn(), tamarind);

        assertThat(bookCopyRepository.findByIsbn(book.isbn())).hasSize(2);
        assertThat(bookCopyRepository.findByBranchId(banyan)).extracting(BookCopy::copyId).containsExactly(banyanCopy.copyId());
        assertThat(bookCopyRepository.findByBranchId(tamarind)).extracting(BookCopy::copyId).containsExactly(tamarindCopy.copyId());
    }

    @Test
    void preventsRemovalWhenAnyCopyIsUnavailable() {
        Book book = catalogService.addBook(bookFactory.create(
                "Domain-Driven Design", "Eric Evans", "9780321125217", 2003, Set.of("architecture")));
        BookCopy available = catalogService.registerPhysicalCopy(book.isbn(), banyan);
        bookCopyRepository.save(available.withStatus(CopyStatus.CHECKED_OUT));

        assertThatThrownBy(() -> catalogService.removeBook(book.isbn()))
                .hasMessageContaining("Cannot remove book");
    }
}
