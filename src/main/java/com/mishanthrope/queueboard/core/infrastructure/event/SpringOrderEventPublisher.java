package com.mishanthrope.queueboard.core.infrastructure.event;

import com.mishanthrope.queueboard.core.application.order.OrderEventPublisher;
import com.mishanthrope.queueboard.core.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes through Spring. Spring Modulith records the publication in the event publication
 * registry inside the command's transaction and delivers it to listeners after commit.
 */
@Component
@RequiredArgsConstructor
public class SpringOrderEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(OrderEvent event) {
        publisher.publishEvent(event);
    }
}
