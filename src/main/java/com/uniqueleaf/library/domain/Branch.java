package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.BranchId;

import java.util.Objects;

public final class Branch {
    private final BranchId branchId;
    private final String name;
    private final String address;

    public Branch(BranchId branchId, String name, String address) {
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.name = normalize(name, "name");
        this.address = normalize(address, "address");
    }

    public BranchId branchId() {
        return branchId;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    private String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Branch " + label + " must not be blank");
        }
        return normalized;
    }
}
