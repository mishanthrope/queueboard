package com.mishanthrope.queueboard.analytics.internal;

import com.mishanthrope.queueboard.analytics.api.OrderAnalytics;
import com.mishanthrope.queueboard.application.order.event.CookingStarted;
import com.mishanthrope.queueboard.application.order.event.OrderAccepted;
import com.mishanthrope.queueboard.application.order.event.OrderCancelled;
import com.mishanthrope.queueboard.application.order.event.OrderEvent;
import com.mishanthrope.queueboard.application.order.event.OrderIssued;
import com.mishanthrope.queueboard.application.order.event.OrderReady;
import com.mishanthrope.queueboard.application.order.event.OrderRestored;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAnalyticsService implements OrderAnalytics {

    private final SpringDataProcessedEventRepository processedEvents;
    private final SpringDataAnalyticsOrderRepository orders;
    private final SpringDataModifierUsageRepository modifierUsage;
    private final SimulationSettings simulation;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OrderEvent event) {
        simulation.apply();
        var inserted = processedEvents.insertIfAbsent(
                event.eventId(), typeOf(event), event.orderId(), event.occurredAt());
        if (inserted == 0) {
            log.info("Analytics: event {} already processed, ignored", event.eventId());
            return;
        }
        switch (event) {
            case OrderAccepted accepted -> apply(accepted);
            case CookingStarted started -> {
                var order = orderFor(started);
                if (order.getStartedAt() == null) {
                    order.setStartedAt(started.startedAt());
                }
                orders.save(order);
            }
            case OrderReady ready -> {
                var order = orderFor(ready);
                order.setReadyAt(ready.readyAt());
                orders.save(order);
            }
            case OrderIssued issued -> log.debug("Analytics: order {} issued", issued.orderId());
            case OrderCancelled cancelled ->
                    log.debug("Analytics: order {} cancelled", cancelled.orderId());
            case OrderRestored restored ->
                    log.debug("Analytics: order {} restored", restored.orderId());
        }
    }

    static String typeOf(OrderEvent event) {
        return switch (event) {
            case OrderAccepted accepted -> "ACCEPTED";
            case CookingStarted started -> "COOKING_STARTED";
            case OrderReady ready -> "READY";
            case OrderIssued issued -> "ISSUED";
            case OrderCancelled cancelled -> "CANCELLED";
            case OrderRestored restored -> "RESTORED";
        };
    }

    private void apply(OrderAccepted accepted) {
        var order = orderFor(accepted);
        order.setPaidAt(accepted.paidAt());
        order.setEstimatedReadyAt(accepted.estimatedReadyAt());
        orders.save(order);
        var day = accepted.occurredAt().atZone(clock.getZone()).toLocalDate();
        Map<String, Long> perModifier = new HashMap<>();
        for (var item : accepted.items()) {
            for (var modifier : item.modifiers()) {
                perModifier.merge(modifier, (long) item.quantity(), Long::sum);
            }
        }
        perModifier.forEach((modifier, times) -> modifierUsage.increment(day, modifier, times));
    }

    private AnalyticsOrderEntity orderFor(OrderEvent event) {
        return orders.findById(event.orderId())
                .orElseGet(() -> new AnalyticsOrderEntity(
                        event.orderId(), event.displayNumber()));
    }
}
