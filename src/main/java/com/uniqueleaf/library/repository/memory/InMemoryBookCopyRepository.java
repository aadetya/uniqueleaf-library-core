package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.repository.BookCopyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookCopyRepository implements BookCopyRepository {
    private final Map<CopyId, BookCopy> storage = new ConcurrentHashMap<>();

    @Override
    public BookCopy save(BookCopy bookCopy) {
        storage.put(bookCopy.copyId(), bookCopy);
        return bookCopy;
    }

    @Override
    public Optional<BookCopy> findById(CopyId copyId) {
        return Optional.ofNullable(storage.get(copyId));
    }

    @Override
    public List<BookCopy> findByIsbn(Isbn isbn) {
        return storage.values().stream()
                .filter(copy -> copy.isbn().equals(isbn))
                .toList();
    }

    @Override
    public List<BookCopy> findByBranchId(BranchId branchId) {
        return storage.values().stream()
                .filter(copy -> copy.branchId().equals(branchId))
                .toList();
    }

    @Override
    public List<BookCopy> findByIsbnAndBranchId(Isbn isbn, BranchId branchId) {
        return storage.values().stream()
                .filter(copy -> copy.isbn().equals(isbn))
                .filter(copy -> copy.branchId().equals(branchId))
                .toList();
    }

    @Override
    public Optional<BookCopy> findFirstByIsbnAndBranchIdAndStatus(Isbn isbn, BranchId branchId, CopyStatus status) {
        return storage.values().stream()
                .filter(copy -> copy.isbn().equals(isbn))
                .filter(copy -> copy.branchId().equals(branchId))
                .filter(copy -> copy.status() == status)
                .findFirst();
    }

    @Override
    public List<BookCopy> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteByIsbn(Isbn isbn) {
        storage.entrySet().removeIf(entry -> entry.getValue().isbn().equals(isbn));
    }
}
