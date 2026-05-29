package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository {
    BookCopy save(BookCopy bookCopy);

    Optional<BookCopy> findById(CopyId copyId);

    List<BookCopy> findByIsbn(Isbn isbn);

    List<BookCopy> findByBranchId(BranchId branchId);

    List<BookCopy> findByIsbnAndBranchId(Isbn isbn, BranchId branchId);

    Optional<BookCopy> findFirstByIsbnAndBranchIdAndStatus(Isbn isbn, BranchId branchId, CopyStatus status);

    List<BookCopy> findAll();

    void deleteByIsbn(Isbn isbn);
}
