package com.mishanthrope.queueboard.analytics.api;

import com.mishanthrope.queueboard.application.order.event.OrderEvent;

/**
 * Contract of the analytics component.
 *
 * <p>Accepts every {@link OrderEvent}. What it does with each:
 * <ul>
 *   <li>{@code OrderAccepted}: remembers the order (paid, estimated time), counts the ordered
 *       modifiers per day, logs the event;</li>
 *   <li>{@code CookingStarted}: remembers the first start time of the order, logs the
 *       event;</li>
 *   <li>{@code OrderReady}: remembers the ready time (the last one wins), logs the event;</li>
 *   <li>{@code OrderIssued}, {@code OrderCancelled}, {@code OrderRestored}: only logged.</li>
 * </ul>
 *
 * <p>Idempotent: an event with an already processed {@code eventId} changes nothing. Runs in its
 * own transaction, so a failure never affects the caller's transaction. May throw; the caller
 * decides what a failure means.
 */
public interface OrderAnalytics {

    void record(OrderEvent event);
}
