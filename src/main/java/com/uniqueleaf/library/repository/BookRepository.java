package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.value.Isbn;

import java.util.List;
import java.util.Optional;

public interface BookRepository {
    Book save(Book book);

    Optional<Book> findByIsbn(Isbn isbn);

    List<Book> findAll();

    boolean existsByIsbn(Isbn isbn);

    void deleteByIsbn(Isbn isbn);
}
