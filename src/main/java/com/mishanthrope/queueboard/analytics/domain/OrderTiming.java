package com.mishanthrope.queueboard.analytics.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * When things happened to one order. {@link #start} keeps the earliest start and
 * {@link #complete} the latest ready time, so the order in which facts arrive does not matter.
 */
public class OrderTiming {

    private final long orderId;
    private final int displayNumber;
    private Instant paidAt;
    private Instant promisedAt;
    private Instant startedAt;
    private Instant readyAt;

    public OrderTiming(long orderId, int displayNumber) {
        this.orderId = orderId;
        this.displayNumber = displayNumber;
    }

    public static OrderTiming reconstitute(
            long orderId,
            int displayNumber,
            Instant paidAt,
            Instant promisedAt,
            Instant startedAt,
            Instant readyAt) {
        var timing = new OrderTiming(orderId, displayNumber);
        timing.paidAt = paidAt;
        timing.promisedAt = promisedAt;
        timing.startedAt = startedAt;
        timing.readyAt = readyAt;
        return timing;
    }

    public void registerPromise(Instant paidAt, Instant promisedAt) {
        this.paidAt = paidAt;
        this.promisedAt = promisedAt;
    }

    public void start(Instant at) {
        if (startedAt == null || at.isBefore(startedAt)) {
            startedAt = at;
        }
    }

    public void complete(Instant at) {
        if (readyAt == null || at.isAfter(readyAt)) {
            readyAt = at;
        }
    }

    /** Start to ready. */
    public Optional<Duration> cookingTime() {
        return between(startedAt, readyAt);
    }

    /** Payment to ready. */
    public Optional<Duration> waitTime() {
        return between(paidAt, readyAt);
    }

    /** Ready minus the promised time: positive means later than promised. */
    public Optional<Duration> estimateError() {
        return between(promisedAt, readyAt);
    }

    private static Optional<Duration> between(Instant from, Instant to) {
        if (from == null || to == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(from, to));
    }

    public long getOrderId() {
        return orderId;
    }

    public int getDisplayNumber() {
        return displayNumber;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getPromisedAt() {
        return promisedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }
}
