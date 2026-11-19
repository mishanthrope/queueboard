package com.mishanthrope.queueboard.domain.employee;

public class EmployeeFactory {

    private static final int MAX_FULL_NAME_LENGTH = 100;

    private final EmployeeRepository employeeRepository;

    public EmployeeFactory(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee create(Email email, String fullName, String passwordHash) {
        String name = fullName == null ? "" : fullName.strip();
        if (name.isEmpty() || name.length() > MAX_FULL_NAME_LENGTH) {
            throw new InvalidEmployeeError(
                    "Full name must be 1 to %d characters".formatted(MAX_FULL_NAME_LENGTH));
        }
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateEmailError(email);
        }
        return new Employee(null, email, passwordHash, name);
    }
}
