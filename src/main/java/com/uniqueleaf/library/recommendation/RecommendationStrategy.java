package com.uniqueleaf.library.recommendation;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.Recommendation;

import java.util.Collection;
import java.util.List;

public interface RecommendationStrategy {
    List<Recommendation> recommend(Patron patron, Collection<Book> candidates, Collection<Book> borrowingHistory);
}
