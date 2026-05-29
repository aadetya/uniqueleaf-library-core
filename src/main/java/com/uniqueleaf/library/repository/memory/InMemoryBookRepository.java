package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.repository.BookRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBookRepository implements BookRepository {
    private final Map<Isbn, Book> storage = new LinkedHashMap<>();

    @Override
    public Book save(Book book) {
        storage.put(book.isbn(), book);
        return book;
    }

    @Override
    public Optional<Book> findByIsbn(Isbn isbn) {
        return Optional.ofNullable(storage.get(isbn));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean existsByIsbn(Isbn isbn) {
        return storage.containsKey(isbn);
    }

    @Override
    public void deleteByIsbn(Isbn isbn) {
        storage.remove(isbn);
    }
}
