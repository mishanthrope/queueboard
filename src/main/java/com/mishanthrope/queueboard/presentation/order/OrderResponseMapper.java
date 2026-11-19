package com.mishanthrope.queueboard.presentation.order;

import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderResponseMapper {

    @Mapping(target = "estimatedReadyAt", expression = "java(estimatedReadyAt(order))")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    default Instant estimatedReadyAt(Order order) {
        return order.getStatus().isFinished() ? null : order.getEstimatedReadyAt();
    }
}
