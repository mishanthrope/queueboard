package com.mishanthrope.queueboard.application.auth;

import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.EmployeeFactory;
import com.mishanthrope.queueboard.domain.employee.EmployeeRepository;
import com.mishanthrope.queueboard.domain.employee.InvalidEmployeeError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFactory employeeFactory;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    @Transactional
    public Employee register(String email, String rawPassword, String fullName) {
        Employee employee = employeeFactory.create(
                new Email(email), fullName, passwordHasher.hash(rawPassword));
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        Email normalized = parseEmail(email);
        Employee employee = employeeRepository.findByEmail(normalized)
                .filter(e -> passwordHasher.matches(rawPassword, e.getPasswordHash()))
                .orElseThrow(InvalidCredentialsError::new);
        return tokenService.issue(employee.getEmail().value());
    }

    private static Email parseEmail(String email) {
        try {
            return new Email(email);
        } catch (InvalidEmployeeError e) {
            throw new InvalidCredentialsError();
        }
    }
}
