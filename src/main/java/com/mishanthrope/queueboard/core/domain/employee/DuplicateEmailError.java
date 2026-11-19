package com.mishanthrope.queueboard.core.domain.employee;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class DuplicateEmailError extends DomainError {

    public DuplicateEmailError(Email email) {
        super("Employee with email %s already exists".formatted(email.value()));
    }
}
