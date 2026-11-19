package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOrderTimingRepository
        extends JpaRepository<OrderTimingEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO analytics_orders (order_id, display_number)
            VALUES (:orderId, :displayNumber)
            ON CONFLICT (order_id) DO NOTHING""", nativeQuery = true)
    void insertIfAbsent(
            @Param("orderId") long orderId, @Param("displayNumber") int displayNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from OrderTimingEntity t where t.orderId = :orderId")
    Optional<OrderTimingEntity> findForUpdate(@Param("orderId") long orderId);

    List<OrderTimingEntity> findByReadyAtGreaterThanEqualAndReadyAtLessThan(
            Instant from, Instant to);
}
