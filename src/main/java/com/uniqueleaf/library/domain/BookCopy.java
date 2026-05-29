package com.uniqueleaf.library.domain;

import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;

import java.util.Objects;

public final class BookCopy {
    private final CopyId copyId;
    private final Isbn isbn;
    private final BranchId branchId;
    private final CopyStatus status;

    public BookCopy(CopyId copyId, Isbn isbn, BranchId branchId, CopyStatus status) {
        this.copyId = Objects.requireNonNull(copyId, "copyId");
        this.isbn = Objects.requireNonNull(isbn, "isbn");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.status = Objects.requireNonNull(status, "status");
    }

    public CopyId copyId() {
        return copyId;
    }

    public Isbn isbn() {
        return isbn;
    }

    public BranchId branchId() {
        return branchId;
    }

    public CopyStatus status() {
        return status;
    }

    public BookCopy moveToBranch(BranchId newBranchId) {
        return new BookCopy(copyId, isbn, newBranchId, status);
    }

    public BookCopy withStatus(CopyStatus newStatus) {
        return new BookCopy(copyId, isbn, branchId, newStatus);
    }

    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }
}
