package com.mishanthrope.queueboard.domain.order;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Order save(Order order) {
        Order stored = order;
        if (order.getId() == null) {
            stored = Order.reconstitute(
                    nextId++,
                    order.getPosterCheckId(),
                    order.getDisplayNumber(),
                    order.getStatus(),
                    order.getComment(),
                    order.getPaidAt(),
                    order.getEstimatedReadyAt(),
                    order.getCreatedAt(),
                    order.getStartedAt(),
                    order.getReadyAt(),
                    order.getIssuedAt(),
                    order.getCancelledAt(),
                    order.getItems());
        }
        orders.put(stored.getId(), stored);
        return stored;
    }

    @Override
    public Optional<Order> findById(long id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public Optional<Order> findByPosterCheckId(long posterCheckId) {
        return orders.values().stream()
                .filter(order -> order.getPosterCheckId() == posterCheckId)
                .findFirst();
    }

    @Override
    public boolean existsByPosterCheckId(long posterCheckId) {
        return findByPosterCheckId(posterCheckId).isPresent();
    }

    @Override
    public List<Order> findInQueue() {
        return sortedByPaidAt(order -> order.getStatus().isInQueue());
    }

    @Override
    public Optional<Instant> findLatestEstimatedReadyAtInQueue() {
        return findInQueue().stream()
                .map(Order::getEstimatedReadyAt)
                .max(Comparator.naturalOrder());
    }

    @Override
    public Optional<Integer> findMaxDisplayNumberCreatedSince(Instant from) {
        return orders.values().stream()
                .filter(order -> !order.getCreatedAt().isBefore(from))
                .map(Order::getDisplayNumber)
                .max(Comparator.naturalOrder());
    }

    private List<Order> sortedByPaidAt(Predicate<Order> filter) {
        return orders.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(Order::getPaidAt))
                .toList();
    }
}
