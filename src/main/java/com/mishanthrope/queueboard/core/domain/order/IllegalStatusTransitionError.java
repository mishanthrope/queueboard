package com.mishanthrope.queueboard.core.domain.order;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class IllegalStatusTransitionError extends DomainError {

    public IllegalStatusTransitionError(String action, OrderStatus status) {
        super("Cannot %s order in status %s".formatted(action, status));
    }
}
