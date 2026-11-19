package com.mishanthrope.queueboard.application.order.event;

import java.time.Instant;
import java.util.UUID;

public record CookingStarted(
        UUID eventId, Instant occurredAt, long orderId, int displayNumber, Instant startedAt)
        implements OrderEvent {
}
