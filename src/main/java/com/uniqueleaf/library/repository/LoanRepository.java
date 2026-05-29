package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.Loan;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.LoanId;
import com.uniqueleaf.library.domain.value.PatronId;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    Loan save(Loan loan);

    Optional<Loan> findById(LoanId loanId);

    List<Loan> findByPatronId(PatronId patronId);

    List<Loan> findActiveByPatronId(PatronId patronId);

    Optional<Loan> findActiveByCopyId(CopyId copyId);

    List<Loan> findAll();
}
