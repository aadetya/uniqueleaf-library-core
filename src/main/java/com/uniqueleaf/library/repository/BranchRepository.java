package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.value.BranchId;

import java.util.List;
import java.util.Optional;

public interface BranchRepository {
    Branch save(Branch branch);

    Optional<Branch> findById(BranchId branchId);

    List<Branch> findAll();
}
