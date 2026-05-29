package com.uniqueleaf.library.factory;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.exception.InvalidDomainDataException;

import java.time.Year;
import java.util.Set;

public class BookFactory {
    public Book create(String title, String author, String rawIsbn, int publicationYear, Set<String> genres) {
        try {
            return rebuild(new Isbn(rawIsbn), title, author, publicationYear, genres);
        } catch (IllegalArgumentException exception) {
            throw new InvalidDomainDataException(exception.getMessage());
        }
    }

    public Book rebuild(Isbn isbn, String title, String author, int publicationYear, Set<String> genres) {
        String normalizedTitle = normalize(title, "title");
        String normalizedAuthor = normalize(author, "author");
        int currentYear = Year.now().getValue();
        if (publicationYear < 1450 || publicationYear > currentYear) {
            throw new InvalidDomainDataException("Publication year is out of range: " + publicationYear);
        }
        return new Book(isbn, normalizedTitle, normalizedAuthor, publicationYear, genres);
    }

    private String normalize(String rawValue, String label) {
        if (rawValue == null) {
            throw new InvalidDomainDataException(label + " must not be null");
        }
        String normalized = rawValue.trim();
        if (normalized.isBlank()) {
            throw new InvalidDomainDataException(label + " must not be blank");
        }
        return normalized;
    }
}
