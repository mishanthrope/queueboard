package com.mishanthrope.queueboard.infrastructure.persistence.order;

import com.mishanthrope.queueboard.application.board.query.BoardRow;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByPosterCheckId(Long posterCheckId);

    boolean existsByPosterCheckId(Long posterCheckId);

    Optional<OrderEntity> findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(
            Instant from);

    List<OrderEntity> findAllByStatusInOrderByPaidAtAsc(Collection<OrderStatus> statuses);

    @Query("select max(o.estimatedReadyAt) from OrderEntity o where o.status in :statuses")
    Optional<Instant> findLatestEstimatedReadyAt(
            @Param("statuses") Collection<OrderStatus> statuses);

    Page<OrderEntity> findAllByStatus(OrderStatus status, Pageable pageable);

    Page<OrderEntity> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Instant from, Instant to, Pageable pageable);

    Page<OrderEntity> findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus status, Instant from, Instant to, Pageable pageable);

    @Query("select new com.mishanthrope.queueboard.application.board.query.BoardRow("
            + "o.displayNumber, o.status, o.estimatedReadyAt) "
            + "from OrderEntity o where o.status in :statuses order by o.paidAt asc")
    List<BoardRow> findBoardRows(@Param("statuses") Collection<OrderStatus> statuses);
}
