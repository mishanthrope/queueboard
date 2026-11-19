package com.mishanthrope.queueboard.core.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderAccepted(
        UUID eventId,
        Instant occurredAt,
        long orderId,
        int displayNumber,
        long posterCheckId,
        Instant paidAt,
        Instant estimatedReadyAt,
        List<Item> items) implements OrderEvent {

    public OrderAccepted {
        items = List.copyOf(items);
    }

    public record Item(String productName, int quantity, List<String> modifiers) {

        public Item {
            modifiers = List.copyOf(modifiers);
        }
    }
}
