package com.mishanthrope.queueboard.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.domain.employee.DuplicateEmailError;
import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.EmployeeFactory;
import com.mishanthrope.queueboard.domain.employee.EmployeeRepository;
import com.mishanthrope.queueboard.domain.employee.InvalidEmployeeError;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Email COOK = new Email("cook@example.com");

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                employeeRepository, new EmployeeFactory(employeeRepository), passwordHasher,
                tokenService);
        lenient().when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(passwordHasher.hash("secret123")).thenReturn("hashed-secret");
    }

    private static Employee savedEmployee() {
        return Employee.reconstitute(1L, COOK, "hashed-secret", "Іван Кухар");
    }

    @Test
    void register_withExistingEmailInOtherCase_throwsDuplicateEmail() {
        when(employeeRepository.existsByEmail(COOK)).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(" Cook@Example.COM ", "secret123", "Іван"))
                .isInstanceOf(DuplicateEmailError.class)
                .hasMessage("Employee with email cook@example.com already exists");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void register_normalizesEmailAndFullNameBeforeSaving() {
        Employee employee = authService.register(
                " Cook@Example.COM ", "secret123", "  Іван Кухар ");

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail().value()).isEqualTo("cook@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Іван Кухар");
        assertThat(employee.getEmail().value()).isEqualTo("cook@example.com");
    }

    @Test
    void register_storesHashFromThePasswordHasherNotThePlainPassword() {
        authService.register("cook@example.com", "secret123", "Іван");

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-secret");
    }

    @Test
    void register_withMalformedEmail_throwsInvalidEmployeeAndSavesNothing() {
        assertThatThrownBy(() -> authService.register("not-an-email", "secret123", "Іван"))
                .isInstanceOf(InvalidEmployeeError.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void register_withBlankFullName_throwsInvalidEmployeeAndSavesNothing() {
        assertThatThrownBy(() -> authService.register("cook@example.com", "secret123", "  "))
                .isInstanceOf(InvalidEmployeeError.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void login_withCorrectCredentials_returnsTokenForNormalizedEmail() {
        when(employeeRepository.findByEmail(COOK)).thenReturn(Optional.of(savedEmployee()));
        when(passwordHasher.matches("secret123", "hashed-secret")).thenReturn(true);
        when(tokenService.issue("cook@example.com")).thenReturn("the-token");

        assertThat(authService.login(" COOK@example.com", "secret123")).isEqualTo("the-token");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsAndIssuesNoToken() {
        when(employeeRepository.findByEmail(COOK)).thenReturn(Optional.of(savedEmployee()));
        when(passwordHasher.matches("wrong-password", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("cook@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issue(anyString());
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials() {
        when(employeeRepository.findByEmail(new Email("nobody@example.com")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "secret123"))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withMalformedEmail_throwsInvalidCredentialsNotInvalidEmployee() {
        assertThatThrownBy(() -> authService.login("not-an-email", "secret123"))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issue(anyString());
    }
}
