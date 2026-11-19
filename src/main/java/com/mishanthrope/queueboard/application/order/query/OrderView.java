package com.mishanthrope.queueboard.application.order.query;

import com.mishanthrope.queueboard.domain.order.OrderStatus;
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
