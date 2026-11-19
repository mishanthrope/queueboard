package com.mishanthrope.queueboard.core.domain.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final List<Employee> employees = new ArrayList<>();

    @Override
    public Employee save(Employee employee) {
        Employee stored = Employee.reconstitute(
                (long) employees.size() + 1,
                employee.getEmail(),
                employee.getPasswordHash(),
                employee.getFullName());
        employees.add(stored);
        return stored;
    }

    @Override
    public Optional<Employee> findByEmail(Email email) {
        return employees.stream().filter(e -> e.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }
}
