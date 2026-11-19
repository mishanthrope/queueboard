package com.mishanthrope.queueboard.core.infrastructure.persistence.order;

import com.mishanthrope.queueboard.core.application.common.PageView;
import com.mishanthrope.queueboard.core.application.order.query.OrderReadRepository;
import com.mishanthrope.queueboard.core.application.order.query.OrderView;
import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
public class OrderReadRepositoryAdapter implements OrderReadRepository {

    private static final Sort HISTORY_SORT =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SpringDataOrderRepository springDataRepository;
    private final OrderViewMapper mapper;
    private final Clock clock;

    @Override
    public List<OrderView> findActive() {
        return springDataRepository.findAllByStatusInOrderByPaidAtAsc(OrderStatusSets.ACTIVE)
                .stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public Optional<OrderView> findById(long id) {
        return springDataRepository.findById(id).map(mapper::toView);
    }

    @Override
    public PageView<OrderView> findHistory(
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
        return new PageView<>(
                entities.getContent().stream().map(mapper::toView).toList(),
                entities.getNumber(),
                entities.getSize(),
                entities.getTotalElements(),
                entities.getTotalPages());
    }
}
