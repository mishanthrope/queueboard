package com.mishanthrope.queueboard.core.domain.employee;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class InvalidEmployeeError extends DomainError {

    public InvalidEmployeeError(String message) {
        super(message);
    }
}
