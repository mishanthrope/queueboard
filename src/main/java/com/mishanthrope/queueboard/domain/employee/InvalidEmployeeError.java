package com.mishanthrope.queueboard.domain.employee;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class InvalidEmployeeError extends DomainError {

    public InvalidEmployeeError(String message) {
        super(message);
    }
}
