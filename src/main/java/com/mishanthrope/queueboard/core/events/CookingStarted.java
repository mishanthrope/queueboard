package com.mishanthrope.queueboard.core.events;

import java.time.Instant;
import java.util.UUID;

public record CookingStarted(
        UUID eventId, Instant occurredAt, long orderId, int displayNumber, Instant startedAt)
        implements OrderEvent {
}
