package com.uniqueleaf.library.domain.value;

import java.util.Objects;

public final class Isbn {
    private final String value;

    public Isbn(String rawValue) {
        String normalized = normalize(rawValue);
        if (!isValidIsbn10(normalized) && !isValidIsbn13(normalized)) {
            throw new IllegalArgumentException("Invalid ISBN: " + rawValue);
        }
        this.value = normalized;
    }

    public String value() {
        return value;
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("ISBN must not be null");
        }
        String normalized = rawValue.replace("-", "").replace(" ", "").trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("ISBN must not be blank");
        }
        return normalized;
    }

    private static boolean isValidIsbn10(String candidate) {
        if (candidate.length() != 10) {
            return false;
        }
        int total = 0;
        for (int index = 0; index < 10; index++) {
            char current = candidate.charAt(index);
            int digit;
            if (index == 9 && current == 'X') {
                digit = 10;
            } else if (Character.isDigit(current)) {
                digit = Character.digit(current, 10);
            } else {
                return false;
            }
            total += (10 - index) * digit;
        }
        return total % 11 == 0;
    }

    private static boolean isValidIsbn13(String candidate) {
        if (candidate.length() != 13 || !candidate.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int total = 0;
        for (int index = 0; index < 12; index++) {
            int digit = Character.digit(candidate.charAt(index), 10);
            total += (index % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (total % 10)) % 10;
        return checkDigit == Character.digit(candidate.charAt(12), 10);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Isbn isbn)) {
            return false;
        }
        return value.equals(isbn.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
