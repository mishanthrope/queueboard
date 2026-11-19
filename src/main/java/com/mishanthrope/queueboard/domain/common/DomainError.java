package com.mishanthrope.queueboard.domain.common;

public abstract class DomainError extends RuntimeException {

    protected DomainError(String message) {
        super(message);
    }
}
