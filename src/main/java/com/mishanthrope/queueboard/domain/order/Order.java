package com.mishanthrope.queueboard.domain.order;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Order {

    private final Long id;
    private final long posterCheckId;
    private final int displayNumber;
    private final String comment;
    private final Instant paidAt;
    private final Instant createdAt;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Instant estimatedReadyAt;
    private Instant startedAt;
    private Instant readyAt;
    private Instant issuedAt;
    private Instant cancelledAt;

    Order(
            Long id,
            long posterCheckId,
            int displayNumber,
            OrderStatus status,
            String comment,
            Instant paidAt,
            Instant estimatedReadyAt,
            Instant createdAt,
            Instant startedAt,
            Instant readyAt,
            Instant issuedAt,
            Instant cancelledAt,
            List<OrderItem> items) {
        this.id = id;
        this.posterCheckId = posterCheckId;
        this.displayNumber = displayNumber;
        this.status = status;
        this.comment = comment;
        this.paidAt = paidAt;
        this.estimatedReadyAt = estimatedReadyAt;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.readyAt = readyAt;
        this.issuedAt = issuedAt;
        this.cancelledAt = cancelledAt;
        this.items = List.copyOf(items);
    }

    /**
     * Rebuilds an order from stored state. For repositories only: it skips the intake rules
     * that {@link OrderFactory} enforces for new orders.
     */
    public static Order reconstitute(
            Long id,
            long posterCheckId,
            int displayNumber,
            OrderStatus status,
            String comment,
            Instant paidAt,
            Instant estimatedReadyAt,
            Instant createdAt,
            Instant startedAt,
            Instant readyAt,
            Instant issuedAt,
            Instant cancelledAt,
            List<OrderItem> items) {
        return new Order(id, posterCheckId, displayNumber, status, comment, paidAt,
                estimatedReadyAt, createdAt, startedAt, readyAt, issuedAt, cancelledAt, items);
    }

    public void startCooking(Instant now) {
        status = status.startCooking();
        startedAt = now;
    }

    public void markReady(Instant now) {
        status = status.markReady();
        readyAt = now;
    }

    public void issue(Instant now) {
        status = status.issue();
        issuedAt = now;
    }

    public void cancel(Instant now) {
        status = status.cancel();
        cancelledAt = now;
    }

    public void restore(Instant now) {
        status = status.restore();
        readyAt = null;
        cancelledAt = null;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    public void shiftEstimate(Duration delta) {
        estimatedReadyAt = estimatedReadyAt.plus(delta);
    }

    public Long getId() {
        return id;
    }

    public long getPosterCheckId() {
        return posterCheckId;
    }

    public int getDisplayNumber() {
        return displayNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
