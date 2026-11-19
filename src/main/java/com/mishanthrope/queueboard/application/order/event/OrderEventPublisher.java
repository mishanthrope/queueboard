package com.mishanthrope.queueboard.application.order.event;

/** Port: announces an order event after the command that caused it has been saved. */
public interface OrderEventPublisher {

    void publish(OrderEvent event);
}
