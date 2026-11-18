package com.mishanthrope.queueboard.order.exception;

import com.mishanthrope.queueboard.common.exception.ConflictException;
import com.mishanthrope.queueboard.order.model.OrderStatus;

public class IllegalStatusTransitionException extends ConflictException {

    public IllegalStatusTransitionException(String action, OrderStatus status) {
        super("Cannot %s order in status %s".formatted(action, status));
    }
}
