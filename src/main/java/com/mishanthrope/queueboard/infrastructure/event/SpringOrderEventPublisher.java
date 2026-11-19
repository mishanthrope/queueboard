package com.mishanthrope.queueboard.infrastructure.event;

import com.mishanthrope.queueboard.application.order.event.OrderEvent;
import com.mishanthrope.queueboard.application.order.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Async mode: a Spring event, delivered to subscribers after the transaction commits. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.analytics.mode", havingValue = "async", matchIfMissing = true)
public class SpringOrderEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(OrderEvent event) {
        publisher.publishEvent(event);
    }
}
