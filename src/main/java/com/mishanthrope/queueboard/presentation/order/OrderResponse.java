package com.mishanthrope.queueboard.presentation.order;

import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
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
        List<OrderItemResponse> items) {
}
