package com.uniqueleaf.library.factory;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookFactoryTest {
    private final BookFactory bookFactory = new BookFactory();

    @Test
    void createsBookWhenDataIsValid() {
        Book book = bookFactory.create(
                "The Pragmatic Programmer",
                "Andrew Hunt",
                "9780201616224",
                1999,
                Set.of("software", "craft"));

        assertThat(book.title()).isEqualTo("The Pragmatic Programmer");
        assertThat(book.author()).isEqualTo("Andrew Hunt");
        assertThat(book.isbn().value()).isEqualTo("9780201616224");
    }

    @Test
    void rejectsInvalidIsbn() {
        assertThatThrownBy(() -> bookFactory.create(
                "Clean Code",
                "Robert C. Martin",
                "12345",
                2008,
                Set.of("software")))
                .isInstanceOf(InvalidDomainDataException.class)
                .hasMessageContaining("Invalid ISBN");
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> bookFactory.create(
                "   ",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Set.of("software")))
                .isInstanceOf(InvalidDomainDataException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsFuturePublicationYear() {
        assertThatThrownBy(() -> bookFactory.create(
                "Domain-Driven Design",
                "Eric Evans",
                "9780321125217",
                Year.now().getValue() + 1,
                Set.of("architecture")))
                .isInstanceOf(InvalidDomainDataException.class)
                .hasMessageContaining("Publication year");
    }
}
