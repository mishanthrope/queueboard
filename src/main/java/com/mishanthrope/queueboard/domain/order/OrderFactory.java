package com.mishanthrope.queueboard.domain.order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class OrderFactory {

    private static final int MAX_COMMENT_LENGTH = 500;

    private final OrderRepository orderRepository;
    private final PrepTimeCalculator prepTimeCalculator;
    private final Duration paidAtMaxSkew;
    private final Clock clock;

    public OrderFactory(
            OrderRepository orderRepository,
            PrepTimeCalculator prepTimeCalculator,
            Duration paidAtMaxSkew,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.prepTimeCalculator = prepTimeCalculator;
        this.paidAtMaxSkew = paidAtMaxSkew;
        this.clock = clock;
    }

    public Order createFromPoster(
            long posterCheckId, Instant paidAt, String comment, List<OrderItem> items) {
        Instant now = clock.instant();
        validateIntake(posterCheckId, paidAt, comment, items, now);
        if (orderRepository.existsByPosterCheckId(posterCheckId)) {
            throw new DuplicatePosterCheckError(posterCheckId);
        }
        return new Order(
                null,
                posterCheckId,
                nextDisplayNumber(now),
                OrderStatus.NEW,
                comment,
                paidAt,
                estimateReadyAt(now, items),
                now,
                null,
                null,
                null,
                null,
                items);
    }

    private void validateIntake(
            long posterCheckId,
            Instant paidAt,
            String comment,
            List<OrderItem> items,
            Instant now) {
        if (posterCheckId <= 0) {
            throw new InvalidOrderError("posterCheckId must be positive");
        }
        if (paidAt.isAfter(now.plus(paidAtMaxSkew))) {
            throw new InvalidOrderError("paidAt must not be in the future");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new InvalidOrderError(
                    "comment must be at most %d characters".formatted(MAX_COMMENT_LENGTH));
        }
        if (items.isEmpty()) {
            throw new InvalidOrderError("Order must contain at least one item");
        }
    }

    private int nextDisplayNumber(Instant now) {
        ZoneId zone = clock.getZone();
        Instant startOfDay = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant();
        return orderRepository.findMaxDisplayNumberCreatedSince(startOfDay).orElse(0) + 1;
    }

    private Instant estimateReadyAt(Instant now, List<OrderItem> items) {
        Instant queueEnd = orderRepository.findLatestEstimatedReadyAtInQueue()
                .filter(latest -> latest.isAfter(now))
                .orElse(now);
        int itemCount = items.stream().mapToInt(OrderItem::quantity).sum();
        return queueEnd.plus(prepTimeCalculator.ownTime(itemCount));
    }
}
