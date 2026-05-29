package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.Isbn;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class Book {
    private final Isbn isbn;
    private final String title;
    private final String author;
    private final int publicationYear;
    private final Set<String> genres;

    public Book(Isbn isbn, String title, String author, int publicationYear, Set<String> genres) {
        this.isbn = Objects.requireNonNull(isbn, "isbn");
        this.title = normalizeText(title, "title");
        this.author = normalizeText(author, "author");
        this.publicationYear = publicationYear;
        this.genres = normalizeGenres(genres);
    }

    public Isbn isbn() {
        return isbn;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public int publicationYear() {
        return publicationYear;
    }

    public Set<String> genres() {
        return genres;
    }

    public Book withMetadata(String newTitle, String newAuthor, int newPublicationYear, Set<String> newGenres) {
        return new Book(isbn, newTitle, newAuthor, newPublicationYear, newGenres);
    }

    public boolean matchesTitle(String query) {
        return title.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    public boolean matchesAuthor(String query) {
        return author.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private Set<String> normalizeGenres(Set<String> genres) {
        TreeSet<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (genres != null) {
            genres.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(normalized::add);
        }
        return Collections.unmodifiableSet(normalized);
    }

    private String normalizeText(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
