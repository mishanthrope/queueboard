package com.mishanthrope.queueboard.core.infrastructure.persistence.order;

import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long posterCheckId;

    @Column(nullable = false)
    private int displayNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private Instant paidAt;

    @Column(nullable = false)
    private Instant estimatedReadyAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant readyAt;

    private Instant issuedAt;

    private Instant cancelledAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id")
    private List<OrderItemEntity> items = new ArrayList<>();
}
