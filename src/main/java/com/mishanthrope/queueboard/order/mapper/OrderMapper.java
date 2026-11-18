package com.mishanthrope.queueboard.order.mapper;

import com.mishanthrope.queueboard.order.dto.OrderItemResponse;
import com.mishanthrope.queueboard.order.dto.OrderResponse;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderItem;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "estimatedReadyAt", source = "estimatedReadyAt")
    OrderResponse toResponse(Order order, Instant estimatedReadyAt);

    OrderItemResponse toItemResponse(OrderItem item);
}
