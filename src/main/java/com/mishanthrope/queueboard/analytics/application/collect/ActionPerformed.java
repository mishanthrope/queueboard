package com.mishanthrope.queueboard.analytics.application.collect;

import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import java.time.Instant;
import java.util.UUID;

/** Any kitchen action other than receiving the order. */
public record ActionPerformed(
        UUID factId, KitchenAction action, long orderId, int displayNumber, Instant at)
        implements KitchenFact {
}
