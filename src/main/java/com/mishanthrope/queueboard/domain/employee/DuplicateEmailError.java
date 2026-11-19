package com.mishanthrope.queueboard.domain.employee;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class DuplicateEmailError extends DomainError {

    public DuplicateEmailError(Email email) {
        super("Employee with email %s already exists".formatted(email.value()));
    }
}
