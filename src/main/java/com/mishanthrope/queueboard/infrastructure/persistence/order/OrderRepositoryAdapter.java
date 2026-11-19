package com.mishanthrope.queueboard.infrastructure.persistence.order;

import com.mishanthrope.queueboard.domain.common.PageResult;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private static final Set<OrderStatus> ACTIVE_STATUSES =
            statusesMatching(OrderStatus::isActive);
    private static final Set<OrderStatus> QUEUE_STATUSES =
            statusesMatching(OrderStatus::isInQueue);

    private static final Sort HISTORY_SORT =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SpringDataOrderRepository springDataRepository;
    private final OrderEntityMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public Order save(Order order) {
        if (order.getId() == null) {
            return mapper.toDomain(springDataRepository.save(mapper.toEntity(order)));
        }
        OrderEntity entity = springDataRepository.findById(order.getId()).orElseThrow();
        mapper.copyMutableFields(order, entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(long id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByPosterCheckId(long posterCheckId) {
        return springDataRepository.findByPosterCheckId(posterCheckId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPosterCheckId(long posterCheckId) {
        return springDataRepository.existsByPosterCheckId(posterCheckId);
    }

    @Override
    public List<Order> findActive() {
        return toDomain(springDataRepository.findAllByStatusInOrderByPaidAtAsc(ACTIVE_STATUSES));
    }

    @Override
    public List<Order> findInQueue() {
        return toDomain(springDataRepository.findAllByStatusInOrderByPaidAtAsc(QUEUE_STATUSES));
    }

    @Override
    public Optional<Instant> findLatestEstimatedReadyAtInQueue() {
        return springDataRepository.findLatestEstimatedReadyAt(QUEUE_STATUSES);
    }

    @Override
    public Optional<Integer> findMaxDisplayNumberCreatedSince(Instant from) {
        return springDataRepository.findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(from)
                .map(OrderEntity::getDisplayNumber);
    }

    @Override
    public PageResult<Order> findHistory(
            LocalDate date, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, HISTORY_SORT);
        Page<OrderEntity> entities;
        if (date == null) {
            entities = status == null
                    ? springDataRepository.findAll(pageable)
                    : springDataRepository.findAllByStatus(status, pageable);
        } else {
            ZoneId zone = clock.getZone();
            Instant from = date.atStartOfDay(zone).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
            entities = status == null
                    ? springDataRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            from, to, pageable)
                    : springDataRepository
                            .findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                    status, from, to, pageable);
        }
        return new PageResult<>(
                toDomain(entities.getContent()),
                entities.getNumber(),
                entities.getSize(),
                entities.getTotalElements(),
                entities.getTotalPages());
    }

    private List<Order> toDomain(List<OrderEntity> entities) {
        return entities.stream().map(mapper::toDomain).toList();
    }

    private static Set<OrderStatus> statusesMatching(Predicate<OrderStatus> predicate) {
        EnumSet<OrderStatus> statuses = EnumSet.noneOf(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            if (predicate.test(status)) {
                statuses.add(status);
            }
        }
        return statuses;
    }
}
