package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.exception.BookNotFoundException;
import com.uniqueleaf.library.exception.BranchNotFoundException;
import com.uniqueleaf.library.exception.LibraryException;
import com.uniqueleaf.library.factory.BookFactory;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.BookRepository;
import com.uniqueleaf.library.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class CatalogService {
    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BranchRepository branchRepository;
    private final BookFactory bookFactory;

    public CatalogService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            BranchRepository branchRepository,
            BookFactory bookFactory) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.branchRepository = branchRepository;
        this.bookFactory = bookFactory;
    }

    public Book addBook(Book book) {
        logger.info("Adding book {} by {}", book.title(), book.author());
        return bookRepository.save(book);
    }

    public Book updateBook(Isbn isbn, String title, String author, int publicationYear, Set<String> genres) {
        bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found for ISBN " + isbn));
        Book updated = bookFactory.rebuild(isbn, title, author, publicationYear, genres);
        logger.info("Updating book {}", isbn);
        return bookRepository.save(updated);
    }

    public void removeBook(Isbn isbn) {
        List<BookCopy> copies = bookCopyRepository.findByIsbn(isbn);
        boolean hasUnavailableCopies = copies.stream().anyMatch(copy -> copy.status() != CopyStatus.AVAILABLE);
        if (hasUnavailableCopies) {
            throw new LibraryException("Cannot remove book with non-available copies: " + isbn);
        }
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException("Book not found for ISBN " + isbn);
        }
        logger.info("Removing book {}", isbn);
        bookCopyRepository.deleteByIsbn(isbn);
        bookRepository.deleteByIsbn(isbn);
    }

    public List<Book> searchByTitle(String query) {
        return bookRepository.findAll().stream()
                .filter(book -> book.matchesTitle(query))
                .toList();
    }

    public List<Book> searchByAuthor(String query) {
        return bookRepository.findAll().stream()
                .filter(book -> book.matchesAuthor(query))
                .toList();
    }

    public Book searchByIsbn(Isbn isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found for ISBN " + isbn));
    }

    public BookCopy registerPhysicalCopy(Isbn isbn, BranchId branchId) {
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException("Cannot register copy for missing ISBN " + isbn);
        }
        if (branchRepository.findById(branchId).isEmpty()) {
            throw new BranchNotFoundException("Cannot register copy at missing branch " + branchId);
        }
        BookCopy copy = new BookCopy(CopyId.random(), isbn, branchId, CopyStatus.AVAILABLE);
        logger.info("Registering copy {} for ISBN {} at branch {}", copy.copyId(), isbn, branchId);
        return bookCopyRepository.save(copy);
    }
}
