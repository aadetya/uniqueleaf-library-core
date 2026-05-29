package com.uniqueleaf.library.domain.value;

import java.util.Objects;
import java.util.UUID;

public final class BranchId {
    private final String value;

    public BranchId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BranchId must not be blank");
        }
        this.value = value.trim();
    }

    public static BranchId random() {
        return new BranchId("BR-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BranchId branchId)) {
            return false;
        }
        return value.equals(branchId.value);
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
