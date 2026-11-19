package com.mishanthrope.queueboard.core.domain.common;

public abstract class DomainError extends RuntimeException {

    protected DomainError(String message) {
        super(message);
    }
}
