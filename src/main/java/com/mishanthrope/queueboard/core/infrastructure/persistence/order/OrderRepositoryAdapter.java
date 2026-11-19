package com.mishanthrope.queueboard.core.infrastructure.persistence.order;

import com.mishanthrope.queueboard.core.domain.order.Order;
import com.mishanthrope.queueboard.core.domain.order.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springDataRepository;
    private final OrderEntityMapper mapper;

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
    public List<Order> findInQueue() {
        return springDataRepository.findAllByStatusInOrderByPaidAtAsc(OrderStatusSets.IN_QUEUE)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Instant> findLatestEstimatedReadyAtInQueue() {
        return springDataRepository.findLatestEstimatedReadyAt(OrderStatusSets.IN_QUEUE);
    }

    @Override
    public Optional<Integer> findMaxDisplayNumberCreatedSince(Instant from) {
        return springDataRepository.findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(
                        from)
                .map(OrderEntity::getDisplayNumber);
    }
}
