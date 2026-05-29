package com.uniqueleaf.library.recommendation;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class GenreAffinityRecommendationStrategy implements RecommendationStrategy {
    @Override
    public List<Recommendation> recommend(Patron patron, Collection<Book> candidates, Collection<Book> borrowingHistory) {
        Set<String> affinityGenres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        affinityGenres.addAll(patron.preferredGenres());
        borrowingHistory.stream()
                .flatMap(book -> book.genres().stream())
                .map(genre -> genre.toLowerCase(Locale.ROOT))
                .forEach(affinityGenres::add);

        return candidates.stream()
                .map(book -> new Recommendation(
                        book,
                        explanation(book, affinityGenres),
                        score(book, affinityGenres),
                        false))
                .sorted(Comparator.comparingInt(Recommendation::score).reversed())
                .toList();
    }

    private int score(Book book, Set<String> preferredGenres) {
        return (int) book.genres().stream()
                .filter(preferred -> preferredGenres.stream().anyMatch(preferred::equalsIgnoreCase))
                .count();
    }

    private String explanation(Book book, Set<String> preferredGenres) {
        long matches = book.genres().stream()
                .filter(preferred -> preferredGenres.stream().anyMatch(preferred::equalsIgnoreCase))
                .count();
        if (matches == 0) {
            return "Discovery pick beyond the current UniqueLeaf genre profile.";
        }
        return "Recommended because it matches " + matches + " genre signal(s) from preferences and borrowing history.";
    }
}
