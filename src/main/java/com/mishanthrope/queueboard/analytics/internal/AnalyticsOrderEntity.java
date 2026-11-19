package com.mishanthrope.queueboard.analytics.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analytics_orders")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsOrderEntity {

    @Id
    @Column(name = "order_id")
    private long orderId;

    @Column(name = "display_number", nullable = false)
    private int displayNumber;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "estimated_ready_at")
    private Instant estimatedReadyAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    AnalyticsOrderEntity(long orderId, int displayNumber) {
        this.orderId = orderId;
        this.displayNumber = displayNumber;
    }
}
