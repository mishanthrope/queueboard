package com.mishanthrope.queueboard.employee.controller;

import com.mishanthrope.queueboard.employee.dto.EmployeeResponse;
import com.mishanthrope.queueboard.employee.dto.LoginRequest;
import com.mishanthrope.queueboard.employee.dto.RegisterRequest;
import com.mishanthrope.queueboard.employee.dto.TokenResponse;
import com.mishanthrope.queueboard.employee.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
