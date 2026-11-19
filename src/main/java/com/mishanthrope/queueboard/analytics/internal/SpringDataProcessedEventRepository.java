package com.mishanthrope.queueboard.analytics.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataProcessedEventRepository
        extends JpaRepository<ProcessedEventEntity, UUID> {

    /** Returns 1 if the event was new, 0 if it had been processed before. */
    @Modifying
    @Query(value = """
            INSERT INTO analytics_processed_events (event_id, event_type, order_id, occurred_at)
            VALUES (:eventId, :eventType, :orderId, :occurredAt)
            ON CONFLICT (event_id) DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("orderId") long orderId,
            @Param("occurredAt") Instant occurredAt);

    long countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            String eventType, Instant from, Instant to);
}
