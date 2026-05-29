package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.repository.LoanRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryLoanRepository implements LoanRepository {
    private final Map<LoanId, Loan> storage = new LinkedHashMap<>();

    @Override
    public Loan save(Loan loan) {
        storage.put(loan.loanId(), loan);
        return loan;
    }

    @Override
    public Optional<Loan> findById(LoanId loanId) {
        return Optional.ofNullable(storage.get(loanId));
    }

    @Override
    public List<Loan> findByPatronId(PatronId patronId) {
        return storage.values().stream()
                .filter(loan -> loan.patronId().equals(patronId))
                .toList();
    }

    @Override
    public List<Loan> findActiveByPatronId(PatronId patronId) {
        return storage.values().stream()
                .filter(Loan::isActive)
                .filter(loan -> loan.patronId().equals(patronId))
                .toList();
    }

    @Override
    public Optional<Loan> findActiveByCopyId(CopyId copyId) {
        return storage.values().stream()
                .filter(Loan::isActive)
                .filter(loan -> loan.copyId().equals(copyId))
                .findFirst();
    }

    @Override
    public List<Loan> findAll() {
        return new ArrayList<>(storage.values());
    }
}
