package com.mishanthrope.queueboard.order.service;

import com.mishanthrope.queueboard.order.config.OrdersProperties;
import com.mishanthrope.queueboard.order.model.OrderItem;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PrepTimeCalculator {

    private final OrdersProperties.PrepTime config;

    public PrepTimeCalculator(OrdersProperties ordersProperties) {
        this.config = ordersProperties.prepTime();
    }

    public Duration ownTime(List<OrderItem> items) {
        int remaining = items.stream().mapToInt(OrderItem::getQuantity).sum();
        Duration wrappedAt = Duration.ZERO;
        Duration grillFreeAt = Duration.ZERO;
        while (remaining > 0) {
            int batch = Math.min(config.grillCapacity(), remaining);
            wrappedAt = wrappedAt.plus(config.wrapPerItem().multipliedBy(batch));
            Duration start = wrappedAt.compareTo(grillFreeAt) >= 0 ? wrappedAt : grillFreeAt;
            grillFreeAt = start.plus(config.grill());
            remaining -= batch;
        }
        return config.overhead().plus(grillFreeAt);
    }
}
