package com.mishanthrope.queueboard.infrastructure.persistence.order;

import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

final class OrderStatusSets {

    static final Set<OrderStatus> ACTIVE = EnumSet.copyOf(
            Arrays.stream(OrderStatus.values()).filter(OrderStatus::isActive).toList());

    static final Set<OrderStatus> IN_QUEUE = EnumSet.copyOf(
            Arrays.stream(OrderStatus.values()).filter(OrderStatus::isInQueue).toList());

    private OrderStatusSets() {
    }
}
