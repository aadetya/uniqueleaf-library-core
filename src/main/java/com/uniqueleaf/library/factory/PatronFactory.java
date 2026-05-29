package com.uniqueleaf.library.factory;

import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.exception.InvalidDomainDataException;

import java.util.Set;
import java.util.regex.Pattern;

public class PatronFactory {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Patron create(String name, String email, Set<String> preferredGenres, BranchId preferredBranchId) {
        return rebuild(PatronId.random(), name, email, preferredGenres, preferredBranchId);
    }

    public Patron rebuild(
            PatronId patronId,
            String name,
            String email,
            Set<String> preferredGenres,
            BranchId preferredBranchId) {
        String normalizedName = normalizeName(name);
        String normalizedEmail = normalizeEmail(email);
        return new Patron(patronId, normalizedName, normalizedEmail, preferredGenres, preferredBranchId);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new InvalidDomainDataException("Patron name must not be blank");
        }
        return name.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            throw new InvalidDomainDataException("Patron email must not be blank");
        }
        String normalized = email.trim();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidDomainDataException("Invalid patron email: " + email);
        }
        return normalized;
    }
}
