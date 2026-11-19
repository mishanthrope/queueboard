package com.mishanthrope.queueboard.core.infrastructure.persistence.order;

import com.mishanthrope.queueboard.core.domain.order.Order;
import com.mishanthrope.queueboard.core.domain.order.OrderItem;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class OrderEntityMapper {

    public Order toDomain(OrderEntity entity) {
        return Order.reconstitute(
                entity.getId(),
                entity.getPosterCheckId(),
                entity.getDisplayNumber(),
                entity.getStatus(),
                entity.getComment(),
                entity.getPaidAt(),
                entity.getEstimatedReadyAt(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getReadyAt(),
                entity.getIssuedAt(),
                entity.getCancelledAt(),
                entity.getItems().stream().map(this::toDomain).toList());
    }

    private OrderItem toDomain(OrderItemEntity entity) {
        return new OrderItem(
                entity.getId(), entity.getProductName(), entity.getQuantity(),
                entity.getModifiers());
    }

    public OrderEntity toEntity(Order order) {
        var entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setPosterCheckId(order.getPosterCheckId());
        entity.setDisplayNumber(order.getDisplayNumber());
        entity.setComment(order.getComment());
        entity.setPaidAt(order.getPaidAt());
        entity.setCreatedAt(order.getCreatedAt());
        copyMutableFields(order, entity);
        order.getItems().forEach(item -> entity.getItems().add(toEntity(item)));
        return entity;
    }

    private OrderItemEntity toEntity(OrderItem item) {
        var entity = new OrderItemEntity();
        entity.setId(item.id());
        entity.setProductName(item.productName());
        entity.setQuantity(item.quantity());
        entity.setModifiers(new ArrayList<>(item.modifiers()));
        return entity;
    }

    public void copyMutableFields(Order order, OrderEntity entity) {
        entity.setStatus(order.getStatus());
        entity.setEstimatedReadyAt(order.getEstimatedReadyAt());
        entity.setStartedAt(order.getStartedAt());
        entity.setReadyAt(order.getReadyAt());
        entity.setIssuedAt(order.getIssuedAt());
        entity.setCancelledAt(order.getCancelledAt());
    }
}
