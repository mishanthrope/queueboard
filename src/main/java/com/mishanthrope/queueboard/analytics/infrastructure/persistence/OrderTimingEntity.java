package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

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
public class OrderTimingEntity {

    @Id
    @Column(name = "order_id")
    private long orderId;

    @Column(name = "display_number", nullable = false)
    private int displayNumber;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "estimated_ready_at")
    private Instant promisedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ready_at")
    private Instant readyAt;
}
