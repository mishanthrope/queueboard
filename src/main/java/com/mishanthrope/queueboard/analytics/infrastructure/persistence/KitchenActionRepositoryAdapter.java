package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import com.mishanthrope.queueboard.analytics.domain.KitchenActionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KitchenActionRepositoryAdapter implements KitchenActionRepository {

    private final SpringDataProcessedEventRepository events;

    @Override
    public boolean registerIfNew(UUID factId, KitchenAction action, long orderId, Instant at) {
        return events.insertIfAbsent(factId, storedType(action), orderId, at) == 1;
    }

    @Override
    public long count(KitchenAction action, Instant from, Instant to) {
        return events.countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                storedType(action), from, to);
    }

    /** The values stored in analytics_processed_events.event_type since Lab 4. */
    static String storedType(KitchenAction action) {
        return switch (action) {
            case RECEIVED -> "ACCEPTED";
            case STARTED -> "COOKING_STARTED";
            case COMPLETED -> "READY";
            case HANDED_OUT -> "ISSUED";
            case CANCELLED -> "CANCELLED";
            case REOPENED -> "RESTORED";
        };
    }
}
