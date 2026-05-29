package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.repository.PatronRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPatronRepository implements PatronRepository {
    private final Map<PatronId, Patron> storage = new LinkedHashMap<>();

    @Override
    public Patron save(Patron patron) {
        storage.put(patron.patronId(), patron);
        return patron;
    }

    @Override
    public Optional<Patron> findById(PatronId patronId) {
        return Optional.ofNullable(storage.get(patronId));
    }

    @Override
    public List<Patron> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean existsById(PatronId patronId) {
        return storage.containsKey(patronId);
    }
}
