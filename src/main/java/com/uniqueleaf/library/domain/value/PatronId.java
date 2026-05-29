package com.uniqueleaf.library.domain.value;

import java.util.Objects;
import java.util.UUID;

public final class PatronId {
    private final String value;

    public PatronId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PatronId must not be blank");
        }
        this.value = value.trim();
    }

    public static PatronId random() {
        return new PatronId("PAT-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PatronId patronId)) {
            return false;
        }
        return value.equals(patronId.value);
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
