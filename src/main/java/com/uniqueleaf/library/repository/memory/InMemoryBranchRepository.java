package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.repository.BranchRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBranchRepository implements BranchRepository {
    private final Map<BranchId, Branch> storage = new LinkedHashMap<>();

    @Override
    public Branch save(Branch branch) {
        storage.put(branch.branchId(), branch);
        return branch;
    }

    @Override
    public Optional<Branch> findById(BranchId branchId) {
        return Optional.ofNullable(storage.get(branchId));
    }

    @Override
    public List<Branch> findAll() {
        return new ArrayList<>(storage.values());
    }
}
