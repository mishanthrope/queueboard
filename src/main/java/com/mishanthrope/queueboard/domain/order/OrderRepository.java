package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.PageResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(long id);

    Optional<Order> findByPosterCheckId(long posterCheckId);

    boolean existsByPosterCheckId(long posterCheckId);

    List<Order> findActive();

    List<Order> findInQueue();

    Optional<Instant> findLatestEstimatedReadyAtInQueue();

    Optional<Integer> findMaxDisplayNumberCreatedSince(Instant from);

    PageResult<Order> findHistory(LocalDate date, OrderStatus status, int page, int size);
}
