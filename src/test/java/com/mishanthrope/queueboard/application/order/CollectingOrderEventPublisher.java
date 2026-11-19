package com.mishanthrope.queueboard.application.order;

import com.mishanthrope.queueboard.application.order.event.OrderEvent;
import com.mishanthrope.queueboard.application.order.event.OrderEventPublisher;
import java.util.ArrayList;
import java.util.List;

/** Fake publisher that remembers everything published. */
public class CollectingOrderEventPublisher implements OrderEventPublisher {

    private final List<OrderEvent> events = new ArrayList<>();

    @Override
    public void publish(OrderEvent event) {
        events.add(event);
    }

    public List<OrderEvent> events() {
        return List.copyOf(events);
    }

    public OrderEvent single() {
        if (events.size() != 1) {
            throw new AssertionError("Expected exactly one event but got " + events);
        }
        return events.get(0);
    }
}
