package com.mishanthrope.queueboard.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.common.exception.ConflictException;
import com.mishanthrope.queueboard.common.security.JwtService;
import com.mishanthrope.queueboard.employee.dto.EmployeeResponse;
import com.mishanthrope.queueboard.employee.dto.LoginRequest;
import com.mishanthrope.queueboard.employee.dto.RegisterRequest;
import com.mishanthrope.queueboard.employee.dto.TokenResponse;
import com.mishanthrope.queueboard.employee.mapper.EmployeeMapperImpl;
import com.mishanthrope.queueboard.employee.model.Employee;
import com.mishanthrope.queueboard.employee.repository.EmployeeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnop";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private EmployeeRepository employeeRepository;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("Europe/Kyiv"));
        jwtService = new JwtService(SECRET, Duration.ofHours(8), clock);
        authService = new AuthService(
                employeeRepository, new EmployeeMapperImpl(), passwordEncoder, jwtService);
    }

    private Employee savedEmployee(String email, String rawPassword) {
        var employee = new Employee();
        employee.setId(1L);
        employee.setEmail(email);
        employee.setFullName("Іван Кухар");
        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        return employee;
    }

    @Test
    void register_withExistingEmailInOtherCase_throwsConflict() {
        when(employeeRepository.existsByEmail("cook@example.com")).thenReturn(true);
        var request = new RegisterRequest(" Cook@Example.COM ", "secret123", "Іван");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void register_normalizesEmailAndFullNameBeforeSaving() {
        when(employeeRepository.existsByEmail("cook@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = authService.register(
                new RegisterRequest(" Cook@Example.COM ", "secret123", "  Іван Кухар "));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("cook@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Іван Кухар");
        assertThat(response.email()).isEqualTo("cook@example.com");
    }

    @Test
    void register_storesPasswordHashedNotPlain() {
        when(employeeRepository.existsByEmail("cook@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest("cook@example.com", "secret123", "Іван"));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        String hash = saved.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", hash)).isTrue();
    }

    @Test
    void login_withCorrectCredentials_returnsTokenForNormalizedEmail() {
        when(employeeRepository.findByEmail("cook@example.com"))
                .thenReturn(Optional.of(savedEmployee("cook@example.com", "secret123")));

        TokenResponse response =
                authService.login(new LoginRequest(" COOK@example.com", "secret123"));

        assertThat(jwtService.extractEmail(response.token())).contains("cook@example.com");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        when(employeeRepository.findByEmail("cook@example.com"))
                .thenReturn(Optional.of(savedEmployee("cook@example.com", "secret123")));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("cook@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throwsBadCredentials() {
        when(employeeRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
