package com.mishanthrope.queueboard.application.common;

public abstract class ApplicationError extends RuntimeException {

    protected ApplicationError(String message) {
        super(message);
    }
}
