package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class DuplicatePosterCheckError extends DomainError {

    public DuplicatePosterCheckError(long posterCheckId) {
        super("Order for Poster check %d already exists".formatted(posterCheckId));
    }
}
