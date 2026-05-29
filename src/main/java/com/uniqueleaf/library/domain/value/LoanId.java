package com.uniqueleaf.library.domain.value;

import java.util.Objects;
import java.util.UUID;

public final class LoanId {
    private final String value;

    public LoanId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LoanId must not be blank");
        }
        this.value = value.trim();
    }

    public static LoanId random() {
        return new LoanId("LN-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LoanId loanId)) {
            return false;
        }
        return value.equals(loanId.value);
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
