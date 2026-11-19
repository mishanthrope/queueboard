package com.mishanthrope.queueboard.core.domain.order;

import com.mishanthrope.queueboard.core.domain.common.DomainError;

public class OrderNotFoundError extends DomainError {

    public OrderNotFoundError(long id) {
        super("Order %d not found".formatted(id));
    }
}
