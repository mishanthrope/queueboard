package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class IllegalStatusTransitionError extends DomainError {

    public IllegalStatusTransitionError(String action, OrderStatus status) {
        super("Cannot %s order in status %s".formatted(action, status));
    }
}
