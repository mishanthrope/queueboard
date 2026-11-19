package com.mishanthrope.queueboard.domain.order;

import com.mishanthrope.queueboard.domain.common.PageResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private final ZoneId zone;
    private long nextId = 1;

    public InMemoryOrderRepository(ZoneId zone) {
        this.zone = zone;
    }

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
    public List<Order> findActive() {
        return sortedByPaidAt(order -> order.getStatus().isActive());
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

    @Override
    public PageResult<Order> findHistory(
            LocalDate date, OrderStatus status, int page, int size) {
        Predicate<Order> inDay = order -> date == null
                || LocalDate.ofInstant(order.getCreatedAt(), zone).equals(date);
        Predicate<Order> withStatus = order -> status == null || order.getStatus() == status;
        List<Order> matching = orders.values().stream()
                .filter(inDay.and(withStatus))
                .sorted(Comparator.comparing(Order::getCreatedAt)
                        .thenComparing(Order::getId)
                        .reversed())
                .toList();
        int from = Math.min(page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        int totalPages = (int) Math.ceil(matching.size() / (double) size);
        return new PageResult<>(matching.subList(from, to), page, size, matching.size(),
                totalPages);
    }

    private List<Order> sortedByPaidAt(Predicate<Order> filter) {
        return orders.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(Order::getPaidAt))
                .toList();
    }
}
