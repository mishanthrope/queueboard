package com.mishanthrope.queueboard.core.infrastructure.persistence.order;

import com.mishanthrope.queueboard.core.application.order.query.OrderItemView;
import com.mishanthrope.queueboard.core.application.order.query.OrderView;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderViewMapper {

    public OrderView toView(OrderEntity entity) {
        Instant estimatedReadyAt = entity.getStatus().isFinished()
                ? null
                : entity.getEstimatedReadyAt();
        return new OrderView(
                entity.getId(),
                entity.getPosterCheckId(),
                entity.getDisplayNumber(),
                entity.getStatus(),
                entity.getComment(),
                entity.getPaidAt(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getReadyAt(),
                entity.getIssuedAt(),
                entity.getCancelledAt(),
                estimatedReadyAt,
                entity.getItems().stream().map(this::toView).toList());
    }

    private OrderItemView toView(OrderItemEntity entity) {
        return new OrderItemView(
                entity.getId(), entity.getProductName(), entity.getQuantity(),
                List.copyOf(entity.getModifiers()));
    }
}
