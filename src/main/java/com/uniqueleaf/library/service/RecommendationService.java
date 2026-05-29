package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.exception.PatronNotFoundException;
import com.uniqueleaf.library.recommendation.RecommendationStrategy;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.BookRepository;
import com.uniqueleaf.library.repository.LoanRepository;
import com.uniqueleaf.library.repository.PatronRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendationService {
    private final PatronRepository patronRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final RecommendationStrategy recommendationStrategy;

    public RecommendationService(
            PatronRepository patronRepository,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository,
            RecommendationStrategy recommendationStrategy) {
        this.patronRepository = patronRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.recommendationStrategy = recommendationStrategy;
    }

    public List<Recommendation> recommend(Patron patron, Collection<Book> candidates, Collection<Book> borrowingHistory) {
        return recommendationStrategy.recommend(patron, candidates, borrowingHistory);
    }

    public List<Recommendation> recommendForPatron(PatronId patronId) {
        return recommendForPatron(patronId, 5);
    }

    public List<Recommendation> recommendForPatron(PatronId patronId, int limit) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new PatronNotFoundException("Patron not found: " + patronId));
        List<Book> borrowingHistory = loanRepository.findByPatronId(patronId).stream()
                .map(Loan::copyId)
                .map(bookCopyRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(copy -> bookRepository.findByIsbn(copy.isbn()))
                .flatMap(java.util.Optional::stream)
                .toList();
        Set<?> borrowedIsbns = borrowingHistory.stream()
                .map(Book::isbn)
                .collect(Collectors.toSet());
        List<Book> candidates = bookRepository.findAll().stream()
                .filter(book -> !borrowedIsbns.contains(book.isbn()))
                .toList();

        return recommendationStrategy.recommend(patron, candidates, borrowingHistory).stream()
                .map(recommendation -> applyAvailabilityContext(recommendation, patron))
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(limit)
                .toList();
    }

    private Recommendation applyAvailabilityContext(Recommendation recommendation, Patron patron) {
        boolean preferredBranchAvailable = patron.preferredBranchId() != null
                && bookCopyRepository.findByIsbnAndBranchId(recommendation.book().isbn(), patron.preferredBranchId()).stream()
                .anyMatch(copy -> copy.status() == CopyStatus.AVAILABLE);
        boolean anyAvailable = bookCopyRepository.findByIsbn(recommendation.book().isbn()).stream()
                .anyMatch(copy -> copy.status() == CopyStatus.AVAILABLE);
        int score = recommendation.score() + (preferredBranchAvailable ? 100 : anyAvailable ? 20 : 0);
        String availabilityNote = preferredBranchAvailable
                ? " Available now at the patron's preferred branch."
                : anyAvailable
                ? " Available in the wider branch network."
                : " Currently unavailable, but kept as a lower-priority discovery suggestion.";
        return new Recommendation(
                recommendation.book(),
                recommendation.explanation() + availabilityNote,
                score,
                preferredBranchAvailable);
    }
}
