package com.mishanthrope.queueboard.order.repository;

import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByPosterCheckId(Long posterCheckId);

    Optional<Order> findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(Instant from);

    List<Order> findAllByStatusInOrderByPaidAtAsc(Collection<OrderStatus> statuses);

    @Query("select max(o.estimatedReadyAt) from Order o where o.status in :statuses")
    Optional<Instant> findLatestEstimatedReadyAt(
            @Param("statuses") Collection<OrderStatus> statuses);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Instant from, Instant to, Pageable pageable);

    Page<Order> findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus status, Instant from, Instant to, Pageable pageable);
}
