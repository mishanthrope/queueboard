package com.mishanthrope.queueboard.analytics.internal;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAnalyticsOrderRepository
        extends JpaRepository<AnalyticsOrderEntity, Long> {

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (ready_at - started_at))) / 60.0 AS cooking,
                   AVG(EXTRACT(EPOCH FROM (ready_at - paid_at))) / 60.0 AS wait,
                   AVG(EXTRACT(EPOCH FROM (ready_at - estimated_ready_at))) / 60.0 AS error
            FROM analytics_orders
            WHERE ready_at >= :from AND ready_at < :to""", nativeQuery = true)
    AverageMinutes averagesForReadyBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Averages in minutes; a value is null when no order had the needed timestamps. */
    interface AverageMinutes {

        Double getCooking();

        Double getWait();

        Double getError();
    }
}
