package com.mishanthrope.queueboard.domain.employee;

public class Employee {

    private final Long id;
    private final Email email;
    private final String passwordHash;
    private final String fullName;

    Employee(Long id, Email email, String passwordHash, String fullName) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public static Employee reconstitute(
            Long id, Email email, String passwordHash, String fullName) {
        return new Employee(id, email, passwordHash, fullName);
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }
}
