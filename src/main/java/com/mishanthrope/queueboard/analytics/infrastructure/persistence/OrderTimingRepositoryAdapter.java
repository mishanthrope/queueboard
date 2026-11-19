package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

import com.mishanthrope.queueboard.analytics.domain.OrderTiming;
import com.mishanthrope.queueboard.analytics.domain.OrderTimingRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderTimingRepositoryAdapter implements OrderTimingRepository {

    private final SpringDataOrderTimingRepository timings;

    @Override
    public OrderTiming getOrCreateLocked(long orderId, int displayNumber) {
        timings.insertIfAbsent(orderId, displayNumber);
        return toDomain(timings.findForUpdate(orderId).orElseThrow());
    }

    @Override
    public void save(OrderTiming timing) {
        var entity = timings.findById(timing.getOrderId()).orElseThrow();
        entity.setPaidAt(timing.getPaidAt());
        entity.setPromisedAt(timing.getPromisedAt());
        entity.setStartedAt(timing.getStartedAt());
        entity.setReadyAt(timing.getReadyAt());
        timings.save(entity);
    }

    @Override
    public List<OrderTiming> findReadyBetween(Instant from, Instant to) {
        return timings.findByReadyAtGreaterThanEqualAndReadyAtLessThan(from, to).stream()
                .map(OrderTimingRepositoryAdapter::toDomain)
                .toList();
    }

    private static OrderTiming toDomain(OrderTimingEntity entity) {
        return OrderTiming.reconstitute(
                entity.getOrderId(),
                entity.getDisplayNumber(),
                entity.getPaidAt(),
                entity.getPromisedAt(),
                entity.getStartedAt(),
                entity.getReadyAt());
    }
}
