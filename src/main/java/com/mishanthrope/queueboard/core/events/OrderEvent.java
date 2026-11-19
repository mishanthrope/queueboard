package com.mishanthrope.queueboard.core.events;

import java.time.Instant;
import java.util.UUID;

/** Something that already happened to an order. Immutable, free of domain types. */
public sealed interface OrderEvent
        permits OrderAccepted, CookingStarted, OrderReady, OrderIssued, OrderCancelled,
        OrderRestored {

    UUID eventId();

    Instant occurredAt();

    long orderId();

    int displayNumber();
}
