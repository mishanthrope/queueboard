package com.mishanthrope.queueboard.analytics.infrastructure.acl;

import com.mishanthrope.queueboard.analytics.application.collect.ActionPerformed;
import com.mishanthrope.queueboard.analytics.application.collect.KitchenFact;
import com.mishanthrope.queueboard.analytics.application.collect.OrderReceived;
import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import com.mishanthrope.queueboard.analytics.domain.ModifierTally;
import com.mishanthrope.queueboard.core.events.CookingStarted;
import com.mishanthrope.queueboard.core.events.OrderAccepted;
import com.mishanthrope.queueboard.core.events.OrderCancelled;
import com.mishanthrope.queueboard.core.events.OrderEvent;
import com.mishanthrope.queueboard.core.events.OrderIssued;
import com.mishanthrope.queueboard.core.events.OrderReady;
import com.mishanthrope.queueboard.core.events.OrderRestored;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The only class that turns Core events into Analytics' own facts. A change in a Core event
 * is absorbed here.
 */
@Component
public class CoreEventTranslator {

    public KitchenFact translate(OrderEvent event) {
        return switch (event) {
            case OrderAccepted accepted -> new OrderReceived(
                    accepted.eventId(),
                    accepted.orderId(),
                    accepted.displayNumber(),
                    accepted.occurredAt(),
                    accepted.paidAt(),
                    accepted.estimatedReadyAt(),
                    tallies(accepted));
            case CookingStarted started -> action(
                    started, KitchenAction.STARTED, started.startedAt());
            case OrderReady ready -> action(ready, KitchenAction.COMPLETED, ready.readyAt());
            case OrderIssued issued -> action(
                    issued, KitchenAction.HANDED_OUT, issued.issuedAt());
            case OrderCancelled cancelled -> action(
                    cancelled, KitchenAction.CANCELLED, cancelled.cancelledAt());
            case OrderRestored restored -> action(
                    restored, KitchenAction.REOPENED, restored.restoredAt());
        };
    }

    private static ActionPerformed action(
            OrderEvent event, KitchenAction action, Instant at) {
        return new ActionPerformed(
                event.eventId(), action, event.orderId(), event.displayNumber(), at);
    }

    /** Modifier counts of the order, each multiplied by the item quantity. */
    private static List<ModifierTally> tallies(OrderAccepted accepted) {
        Map<String, Long> perModifier = new LinkedHashMap<>();
        for (var item : accepted.items()) {
            for (var modifier : item.modifiers()) {
                perModifier.merge(modifier, (long) item.quantity(), Long::sum);
            }
        }
        return perModifier.entrySet().stream()
                .map(entry -> new ModifierTally(entry.getKey(), entry.getValue()))
                .toList();
    }
}
