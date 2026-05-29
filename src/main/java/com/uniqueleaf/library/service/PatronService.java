package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.exception.PatronNotFoundException;
import com.uniqueleaf.library.factory.PatronFactory;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.BookRepository;
import com.uniqueleaf.library.repository.LoanRepository;
import com.uniqueleaf.library.repository.PatronRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class PatronService {
    private static final Logger logger = LoggerFactory.getLogger(PatronService.class);

    private final PatronRepository patronRepository;
    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final PatronFactory patronFactory;

    public PatronService(
            PatronRepository patronRepository,
            LoanRepository loanRepository,
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            PatronFactory patronFactory) {
        this.patronRepository = patronRepository;
        this.loanRepository = loanRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.patronFactory = patronFactory;
    }

    public Patron registerPatron(Patron patron) {
        logger.info("Registering patron {}", patron.name());
        return patronRepository.save(patron);
    }

    public Patron updatePatronDetails(
            PatronId patronId,
            String name,
            String email,
            Set<String> preferredGenres,
            BranchId preferredBranchId) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new PatronNotFoundException("Patron not found: " + patronId));
        Patron updated = patronFactory.rebuild(patron.patronId(), name, email, preferredGenres, preferredBranchId);
        logger.info("Updating details for patron {}", patronId);
        return patronRepository.save(updated);
    }

    public Patron updatePreferences(PatronId patronId, Set<String> preferredGenres, BranchId preferredBranchId) {
        Patron patron = findPatron(patronId);
        return updatePatronDetails(patronId, patron.name(), patron.email(), preferredGenres, preferredBranchId);
    }

    public List<Patron> findAllPatrons() {
        return patronRepository.findAll();
    }

    public Patron findPatron(PatronId patronId) {
        return patronRepository.findById(patronId)
                .orElseThrow(() -> new PatronNotFoundException("Patron not found: " + patronId));
    }

    public List<Loan> borrowingHistory(PatronId patronId) {
        ensurePatronExists(patronId);
        return loanRepository.findByPatronId(patronId);
    }

    public List<Book> borrowingHistoryBooks(PatronId patronId) {
        ensurePatronExists(patronId);
        return loanRepository.findByPatronId(patronId).stream()
                .map(Loan::copyId)
                .map(bookCopyRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(copy -> bookRepository.findByIsbn(copy.isbn()))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private void ensurePatronExists(PatronId patronId) {
        if (!patronRepository.existsById(patronId)) {
            throw new PatronNotFoundException("Patron not found: " + patronId);
        }
    }
}
