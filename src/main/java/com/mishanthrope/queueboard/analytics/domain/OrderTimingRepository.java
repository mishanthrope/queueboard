package com.mishanthrope.queueboard.analytics.domain;

import java.time.Instant;
import java.util.List;

public interface OrderTimingRepository {

    /**
     * Returns the timing of the order, creating it first if it is missing, and locks it so that
     * concurrent facts about the same order are applied one after another.
     */
    OrderTiming getOrCreateLocked(long orderId, int displayNumber);

    void save(OrderTiming timing);

    /** Orders that became ready in the half-open interval. */
    List<OrderTiming> findReadyBetween(Instant from, Instant to);
}
