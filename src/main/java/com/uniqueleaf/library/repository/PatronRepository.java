package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.PatronId;

import java.util.List;
import java.util.Optional;

public interface PatronRepository {
    Patron save(Patron patron);

    Optional<Patron> findById(PatronId patronId);

    List<Patron> findAll();

    boolean existsById(PatronId patronId);
}
