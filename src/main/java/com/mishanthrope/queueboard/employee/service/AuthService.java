package com.mishanthrope.queueboard.employee.service;

import com.mishanthrope.queueboard.common.exception.ConflictException;
import com.mishanthrope.queueboard.common.security.JwtService;
import com.mishanthrope.queueboard.employee.dto.EmployeeResponse;
import com.mishanthrope.queueboard.employee.dto.LoginRequest;
import com.mishanthrope.queueboard.employee.dto.RegisterRequest;
import com.mishanthrope.queueboard.employee.dto.TokenResponse;
import com.mishanthrope.queueboard.employee.mapper.EmployeeMapper;
import com.mishanthrope.queueboard.employee.model.Employee;
import com.mishanthrope.queueboard.employee.repository.EmployeeRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public EmployeeResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("Employee with email %s already exists".formatted(email));
        }
        Employee employee = employeeMapper.toEntity(request);
        employee.setEmail(email);
        employee.setFullName(request.fullName().strip());
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(normalizeEmail(request.email()))
                .filter(e -> passwordEncoder.matches(request.password(), e.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new TokenResponse(jwtService.generateToken(employee.getEmail()));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
