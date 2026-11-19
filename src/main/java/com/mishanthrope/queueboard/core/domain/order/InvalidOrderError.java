package com.mishanthrope.queueboard.core.domain.order;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class InvalidOrderError extends DomainError {

    public InvalidOrderError(String message) {
        super(message);
    }
}
