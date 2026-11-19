package com.mishanthrope.queueboard.application.auth.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.application.auth.InvalidCredentialsError;
import com.mishanthrope.queueboard.application.auth.PasswordHasher;
import com.mishanthrope.queueboard.application.auth.TokenService;
import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.InMemoryEmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenService tokenService;

    private LoginHandler handler;

    @BeforeEach
    void setUp() {
        var repository = new InMemoryEmployeeRepository();
        repository.save(Employee.reconstitute(
                null, new Email("cook@example.com"), "hashed-secret", "Іван Кухар"));
        handler = new LoginHandler(repository, passwordHasher, tokenService);
    }

    @Test
    void handle_withCorrectCredentials_returnsTokenForNormalizedEmail() {
        when(passwordHasher.matches("secret123", "hashed-secret")).thenReturn(true);
        when(tokenService.issue("cook@example.com")).thenReturn("the-token");

        AuthToken token = handler.handle(new Login(" COOK@example.com", "secret123"));

        assertThat(token.token()).isEqualTo("the-token");
    }

    @Test
    void handle_withWrongPassword_throwsInvalidCredentialsAndIssuesNoToken() {
        when(passwordHasher.matches("wrong-password", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new Login("cook@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issue(anyString());
    }

    @Test
    void handle_withUnknownEmail_throwsInvalidCredentials() {
        assertThatThrownBy(() -> handler.handle(new Login("nobody@example.com", "secret123")))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void handle_withMalformedEmail_throwsInvalidCredentialsNotInvalidEmployee() {
        assertThatThrownBy(() -> handler.handle(new Login("not-an-email", "secret123")))
                .isInstanceOf(InvalidCredentialsError.class)
                .hasMessage("Invalid email or password");

        verify(tokenService, never()).issue(anyString());
    }
}
