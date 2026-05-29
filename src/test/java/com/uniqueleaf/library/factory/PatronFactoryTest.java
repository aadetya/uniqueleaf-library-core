package com.uniqueleaf.library.factory;

import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatronFactoryTest {
    private final PatronFactory patronFactory = new PatronFactory();

    @Test
    void createsPatronWhenDataIsValid() {
        Patron patron = patronFactory.create(
                "Asha Mehta",
                "asha@example.com",
                Set.of("architecture", "design"),
                new BranchId("BR-BANYAN"));

        assertThat(patron.name()).isEqualTo("Asha Mehta");
        assertThat(patron.email()).isEqualTo("asha@example.com");
        assertThat(patron.preferredGenres()).contains("architecture", "design");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> patronFactory.create(
                "   ",
                "asha@example.com",
                Set.of("design"),
                new BranchId("BR-BANYAN")))
                .isInstanceOf(InvalidDomainDataException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> patronFactory.create(
                "Kabir Sen",
                "not-an-email",
                Set.of("design"),
                new BranchId("BR-BANYAN")))
                .isInstanceOf(InvalidDomainDataException.class)
                .hasMessageContaining("Invalid patron email");
    }
}
