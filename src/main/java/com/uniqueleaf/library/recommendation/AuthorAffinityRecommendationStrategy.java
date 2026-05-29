package com.uniqueleaf.library.recommendation;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthorAffinityRecommendationStrategy implements RecommendationStrategy {
    @Override
    public List<Recommendation> recommend(Patron patron, Collection<Book> candidates, Collection<Book> borrowingHistory) {
        Map<String, Long> authorFrequency = borrowingHistory.stream()
                .collect(Collectors.groupingBy(Book::author, Collectors.counting()));

        return candidates.stream()
                .map(book -> {
                    int score = authorFrequency.getOrDefault(book.author(), 0L).intValue() * 3;
                    String explanation = score == 0
                            ? "Discovery pick because there is no strong author history match yet."
                            : "Recommended because you previously borrowed books by " + book.author() + ".";
                    return new Recommendation(book, explanation, score, false);
                })
                .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                        .thenComparing(recommendation -> recommendation.book().title()))
                .toList();
    }
}
