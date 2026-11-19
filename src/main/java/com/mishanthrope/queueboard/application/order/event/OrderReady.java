package com.mishanthrope.queueboard.application.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderReady(
        UUID eventId, Instant occurredAt, long orderId, int displayNumber, Instant readyAt)
        implements OrderEvent {
}
