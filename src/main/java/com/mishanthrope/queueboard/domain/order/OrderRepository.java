package com.mishanthrope.queueboard.domain.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(long id);

    default Order getById(long id) {
        return findById(id).orElseThrow(() -> new OrderNotFoundError(id));
    }

    Optional<Order> findByPosterCheckId(long posterCheckId);

    boolean existsByPosterCheckId(long posterCheckId);

    List<Order> findInQueue();

    Optional<Instant> findLatestEstimatedReadyAtInQueue();

    Optional<Integer> findMaxDisplayNumberCreatedSince(Instant from);
}
