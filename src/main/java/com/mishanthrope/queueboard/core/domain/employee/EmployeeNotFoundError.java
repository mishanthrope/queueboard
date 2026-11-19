package com.mishanthrope.queueboard.core.domain.employee;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class EmployeeNotFoundError extends DomainError {

    public EmployeeNotFoundError(long id) {
        super("Employee %d not found".formatted(id));
    }
}
