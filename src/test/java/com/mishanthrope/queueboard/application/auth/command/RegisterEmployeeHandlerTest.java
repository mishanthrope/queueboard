package com.mishanthrope.queueboard.application.auth.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.application.auth.PasswordHasher;
import com.mishanthrope.queueboard.domain.employee.DuplicateEmailError;
import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.EmployeeFactory;
import com.mishanthrope.queueboard.domain.employee.InMemoryEmployeeRepository;
import com.mishanthrope.queueboard.domain.employee.InvalidEmployeeError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterEmployeeHandlerTest {

    private static final Email COOK = new Email("cook@example.com");

    @Mock
    private PasswordHasher passwordHasher;

    private InMemoryEmployeeRepository repository;
    private RegisterEmployeeHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEmployeeRepository();
        handler = new RegisterEmployeeHandler(
                repository, new EmployeeFactory(repository), passwordHasher);
    }

    @Test
    void handle_returnsIdOfTheNewEmployee() {
        when(passwordHasher.hash("secret123")).thenReturn("hashed");

        long id = handler.handle(new RegisterEmployee("cook@example.com", "secret123", "Іван"));

        assertThat(id).isEqualTo(repository.findByEmail(COOK).orElseThrow().getId());
    }

    @Test
    void handle_normalizesEmailAndFullNameBeforeSaving() {
        when(passwordHasher.hash("secret123")).thenReturn("hashed");

        handler.handle(new RegisterEmployee(" Cook@Example.COM ", "secret123", "  Іван Кухар "));

        Employee saved = repository.findByEmail(COOK).orElseThrow();
        assertThat(saved.getEmail().value()).isEqualTo("cook@example.com");
        assertThat(saved.getFullName()).isEqualTo("Іван Кухар");
    }

    @Test
    void handle_storesHashFromThePasswordHasherNotThePlainPassword() {
        when(passwordHasher.hash("secret123")).thenReturn("hashed-secret");

        handler.handle(new RegisterEmployee("cook@example.com", "secret123", "Іван"));

        assertThat(repository.findByEmail(COOK).orElseThrow().getPasswordHash())
                .isEqualTo("hashed-secret");
    }

    @Test
    void handle_withExistingEmailInOtherCase_throwsDuplicateEmailAndKeepsFirstEmployee() {
        when(passwordHasher.hash("secret123")).thenReturn("hashed");
        handler.handle(new RegisterEmployee("cook@example.com", "secret123", "Іван"));

        assertThatThrownBy(() -> handler.handle(
                new RegisterEmployee(" Cook@Example.COM ", "secret123", "Петро")))
                .isInstanceOf(DuplicateEmailError.class)
                .hasMessage("Employee with email cook@example.com already exists");

        assertThat(repository.findByEmail(COOK).orElseThrow().getFullName()).isEqualTo("Іван");
    }

    @Test
    void handle_withMalformedEmail_throwsInvalidEmployeeAndSavesNothing() {
        assertThatThrownBy(() -> handler.handle(
                new RegisterEmployee("not-an-email", "secret123", "Іван")))
                .isInstanceOf(InvalidEmployeeError.class);
    }

    @Test
    void handle_withBlankFullName_throwsInvalidEmployeeAndSavesNothing() {
        when(passwordHasher.hash("secret123")).thenReturn("hashed");

        assertThatThrownBy(() -> handler.handle(
                new RegisterEmployee("cook@example.com", "secret123", "  ")))
                .isInstanceOf(InvalidEmployeeError.class);

        assertThat(repository.existsByEmail(COOK)).isFalse();
    }
}
