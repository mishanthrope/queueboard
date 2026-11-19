package com.mishanthrope.queueboard.domain.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmployeeFactoryTest {

    private static final Email EMAIL = new Email("cook@example.com");

    private InMemoryEmployeeRepository repository;
    private EmployeeFactory factory;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEmployeeRepository();
        factory = new EmployeeFactory(repository);
    }

    @Test
    void create_buildsNewEmployeeWithStrippedFullName() {
        Employee employee = factory.create(EMAIL, "  Іван Кухар ", "hash");

        assertThat(employee.getId()).isNull();
        assertThat(employee.getEmail()).isEqualTo(EMAIL);
        assertThat(employee.getFullName()).isEqualTo("Іван Кухар");
        assertThat(employee.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void create_doesNotSaveTheEmployee() {
        factory.create(EMAIL, "Іван", "hash");

        assertThat(repository.existsByEmail(EMAIL)).isFalse();
    }

    @Test
    void create_withFullNameOfExactlyHundredCharacters_succeeds() {
        Employee employee = factory.create(EMAIL, "x".repeat(100), "hash");

        assertThat(employee.getFullName()).hasSize(100);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void create_withBlankFullName_throwsInvalidEmployee(String fullName) {
        assertThatThrownBy(() -> factory.create(EMAIL, fullName, "hash"))
                .isInstanceOf(InvalidEmployeeError.class)
                .hasMessage("Full name must be 1 to 100 characters");
    }

    @Test
    void create_withTooLongFullName_throwsInvalidEmployee() {
        assertThatThrownBy(() -> factory.create(EMAIL, "x".repeat(101), "hash"))
                .isInstanceOf(InvalidEmployeeError.class);
    }

    @Test
    void create_whenEmailIsTaken_throwsDuplicateEmail() {
        repository.save(factory.create(EMAIL, "Іван", "hash"));

        assertThatThrownBy(() -> factory.create(new Email(" COOK@Example.com "), "Петро", "h2"))
                .isInstanceOf(DuplicateEmailError.class)
                .hasMessage("Employee with email cook@example.com already exists");
    }
}
