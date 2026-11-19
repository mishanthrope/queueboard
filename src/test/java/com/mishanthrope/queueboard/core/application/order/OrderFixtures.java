package com.mishanthrope.queueboard.core.application.order;

import com.mishanthrope.queueboard.core.domain.order.InMemoryOrderRepository;
import com.mishanthrope.queueboard.core.domain.order.Order;
import com.mishanthrope.queueboard.core.domain.order.OrderItem;
import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public final class OrderFixtures {

    private OrderFixtures() {
    }

    public static Order seed(
            InMemoryOrderRepository repository,
            long posterCheckId,
            int displayNumber,
            OrderStatus status,
            Instant createdAt,
            Instant estimatedReadyAt) {
        return repository.save(Order.reconstitute(
                null, posterCheckId, displayNumber, status, null, createdAt.minusSeconds(5),
                estimatedReadyAt, createdAt, null, null, null, null,
                List.of(new OrderItem(null, "Шаурма", 1, List.of()))));
    }
}
