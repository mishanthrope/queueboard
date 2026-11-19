package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.DomainError;

public class OrderNotFoundError extends DomainError {

    public OrderNotFoundError(long id) {
        super("Order %d not found".formatted(id));
    }
}
