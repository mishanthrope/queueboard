package com.mishanthrope.queueboard.domain.employee;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class EmployeeNotFoundError extends DomainError {

    public EmployeeNotFoundError(long id) {
        super("Employee %d not found".formatted(id));
    }
}
