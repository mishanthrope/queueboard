package com.mishanthrope.queueboard.core.application.auth.query;

import com.mishanthrope.queueboard.core.application.auth.InvalidCredentialsError;
import com.mishanthrope.queueboard.core.application.auth.PasswordHasher;
import com.mishanthrope.queueboard.core.application.auth.TokenService;
import com.mishanthrope.queueboard.core.application.common.QueryHandler;
import com.mishanthrope.queueboard.core.domain.employee.Email;
import com.mishanthrope.queueboard.core.domain.employee.Employee;
import com.mishanthrope.queueboard.core.domain.employee.EmployeeRepository;
import com.mishanthrope.queueboard.core.domain.employee.InvalidEmployeeError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginHandler implements QueryHandler<Login, AuthToken> {

    private final EmployeeRepository employeeRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    @Override
    public AuthToken handle(Login query) {
        Email email = parseEmail(query.email());
        Employee employee = employeeRepository.findByEmail(email)
                .filter(e -> passwordHasher.matches(query.password(), e.getPasswordHash()))
                .orElseThrow(InvalidCredentialsError::new);
        return new AuthToken(tokenService.issue(employee.getEmail().value()));
    }

    private static Email parseEmail(String email) {
        try {
            return new Email(email);
        } catch (InvalidEmployeeError e) {
            throw new InvalidCredentialsError();
        }
    }
}
