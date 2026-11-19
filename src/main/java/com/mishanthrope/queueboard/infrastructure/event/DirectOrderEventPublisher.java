package com.mishanthrope.queueboard.infrastructure.event;

import com.mishanthrope.queueboard.analytics.api.OrderAnalytics;
import com.mishanthrope.queueboard.application.order.event.OrderEvent;
import com.mishanthrope.queueboard.application.order.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sync mode: analytics runs in the caller's thread. Failure policy is "ignore": broken
 * statistics must never stop an order from moving on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.analytics.mode", havingValue = "sync")
public class DirectOrderEventPublisher implements OrderEventPublisher {

    private final OrderAnalytics analytics;

    @Override
    public void publish(OrderEvent event) {
        try {
            analytics.record(event);
        } catch (RuntimeException e) {
            log.warn("Analytics failed for event {}, command continues: {}",
                    event.eventId(), e.toString());
        }
    }
}
