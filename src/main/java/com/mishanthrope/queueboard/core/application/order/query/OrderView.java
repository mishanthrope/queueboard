package com.mishanthrope.queueboard.core.application.order.query;

import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderView(
        Long id,
        Long posterCheckId,
        int displayNumber,
        OrderStatus status,
        String comment,
        Instant paidAt,
        Instant createdAt,
        Instant startedAt,
        Instant readyAt,
        Instant issuedAt,
        Instant cancelledAt,
        Instant estimatedReadyAt,
        List<OrderItemView> items) {
}
