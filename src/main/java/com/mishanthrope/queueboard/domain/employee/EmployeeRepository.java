package com.mishanthrope.queueboard.domain.employee;

import java.util.Optional;

public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
