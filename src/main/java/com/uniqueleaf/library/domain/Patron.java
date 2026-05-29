package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.PatronId;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class Patron {
    private final PatronId patronId;
    private final String name;
    private final String email;
    private final Set<String> preferredGenres;
    private final BranchId preferredBranchId;

    public Patron(PatronId patronId, String name, String email, Set<String> preferredGenres, BranchId preferredBranchId) {
        this.patronId = Objects.requireNonNull(patronId, "patronId");
        this.name = normalizeText(name, "name");
        this.email = normalizeText(email, "email");
        this.preferredGenres = normalizeGenres(preferredGenres);
        this.preferredBranchId = preferredBranchId;
    }

    public PatronId patronId() {
        return patronId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public Set<String> preferredGenres() {
        return preferredGenres;
    }

    public BranchId preferredBranchId() {
        return preferredBranchId;
    }

    public Patron withProfile(String updatedName, String updatedEmail, Set<String> genres, BranchId branchId) {
        return new Patron(patronId, updatedName, updatedEmail, genres, branchId);
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
            throw new IllegalArgumentException("Patron " + label + " must not be null");
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Patron " + label + " must not be blank");
        }
        return normalized;
    }
}
