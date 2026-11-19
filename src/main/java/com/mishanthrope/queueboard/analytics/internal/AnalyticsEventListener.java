package com.mishanthrope.queueboard.analytics.internal;

import com.mishanthrope.queueboard.analytics.api.OrderAnalytics;
import com.mishanthrope.queueboard.application.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Async mode subscriber: runs only after the publishing transaction has committed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final OrderAnalytics analytics;

    @Async("analyticsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        try {
            analytics.record(event);
        } catch (RuntimeException e) {
            log.warn("Analytics failed for event {}: {}", event.eventId(), e.toString());
        }
    }
}
