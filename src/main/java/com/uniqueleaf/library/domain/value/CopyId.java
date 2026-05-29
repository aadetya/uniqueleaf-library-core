package com.uniqueleaf.library.domain.value;

import java.util.Objects;
import java.util.UUID;

public final class CopyId {
    private final String value;

    public CopyId(String value) {
        this.value = requireValue(value, "CopyId");
    }

    public static CopyId random() {
        return new CopyId("LC-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    private static String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CopyId copyId)) {
            return false;
        }
        return value.equals(copyId.value);
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
