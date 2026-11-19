package com.mishanthrope.queueboard.presentation.auth;

import com.mishanthrope.queueboard.application.auth.command.RegisterEmployee;
import com.mishanthrope.queueboard.application.auth.command.RegisterEmployeeHandler;
import com.mishanthrope.queueboard.application.auth.query.AuthToken;
import com.mishanthrope.queueboard.application.auth.query.EmployeeView;
import com.mishanthrope.queueboard.application.auth.query.GetEmployee;
import com.mishanthrope.queueboard.application.auth.query.GetEmployeeHandler;
import com.mishanthrope.queueboard.application.auth.query.Login;
import com.mishanthrope.queueboard.application.auth.query.LoginHandler;
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

    private final RegisterEmployeeHandler registerEmployee;
    private final GetEmployeeHandler getEmployee;
    private final LoginHandler login;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeView register(@Valid @RequestBody RegisterRequest request) {
        long id = registerEmployee.handle(
                new RegisterEmployee(request.email(), request.password(), request.fullName()));
        return getEmployee.handle(new GetEmployee(id));
    }

    @PostMapping("/login")
    public AuthToken login(@Valid @RequestBody LoginRequest request) {
        return login.handle(new Login(request.email(), request.password()));
    }
}
