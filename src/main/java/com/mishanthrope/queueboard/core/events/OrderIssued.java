package com.mishanthrope.queueboard.core.events;

import java.time.Instant;
import java.util.UUID;

public record OrderIssued(
        UUID eventId, Instant occurredAt, long orderId, int displayNumber, Instant issuedAt)
        implements OrderEvent {
}
