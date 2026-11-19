package com.mishanthrope.queueboard.core.application.order;

import com.mishanthrope.queueboard.core.events.OrderEvent;

/** Port: announces an order event after the command that caused it has been saved. */
public interface OrderEventPublisher {

    void publish(OrderEvent event);
}
