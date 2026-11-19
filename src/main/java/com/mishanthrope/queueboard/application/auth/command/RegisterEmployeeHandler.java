package com.mishanthrope.queueboard.application.auth.command;

import com.mishanthrope.queueboard.application.auth.PasswordHasher;
import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.EmployeeFactory;
import com.mishanthrope.queueboard.domain.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterEmployeeHandler {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFactory employeeFactory;
    private final PasswordHasher passwordHasher;

    @Transactional
    public long handle(RegisterEmployee command) {
        Employee employee = employeeFactory.create(
                new Email(command.email()),
                command.fullName(),
                passwordHasher.hash(command.password()));
        return employeeRepository.save(employee).getId();
    }
}
