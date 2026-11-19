package com.mishanthrope.queueboard.analytics.application.collect;

import com.mishanthrope.queueboard.analytics.domain.ModifierTally;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderReceived(
        UUID factId,
        long orderId,
        int displayNumber,
        Instant at,
        Instant paidAt,
        Instant promisedAt,
        List<ModifierTally> modifiers) implements KitchenFact {

    public OrderReceived {
        modifiers = List.copyOf(modifiers);
    }
}
