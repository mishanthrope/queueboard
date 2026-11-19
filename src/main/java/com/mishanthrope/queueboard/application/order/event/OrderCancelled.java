package com.mishanthrope.queueboard.application.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelled(
        UUID eventId, Instant occurredAt, long orderId, int displayNumber, Instant cancelledAt)
        implements OrderEvent {
}
