package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class InvalidOrderError extends DomainError {

    public InvalidOrderError(String message) {
        super(message);
    }
}
