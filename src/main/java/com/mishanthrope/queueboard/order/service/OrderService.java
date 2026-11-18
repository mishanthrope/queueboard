package com.mishanthrope.queueboard.order.service;

import com.mishanthrope.queueboard.common.exception.BadRequestException;
import com.mishanthrope.queueboard.common.exception.NotFoundException;
import com.mishanthrope.queueboard.common.web.PageResponse;
import com.mishanthrope.queueboard.order.config.OrdersProperties;
import com.mishanthrope.queueboard.order.dto.OrderResponse;
import com.mishanthrope.queueboard.order.mapper.OrderMapper;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderItem;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    public static final Set<OrderStatus> KITCHEN_STATUSES =
            EnumSet.of(OrderStatus.NEW, OrderStatus.COOKING, OrderStatus.READY);

    private static final Set<OrderStatus> QUEUE_STATUSES =
            EnumSet.of(OrderStatus.NEW, OrderStatus.COOKING);

    private static final Set<OrderStatus> FINISHED_STATUSES =
            EnumSet.of(OrderStatus.ISSUED, OrderStatus.CANCELLED);

    private static final int MAX_COMMENT_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_QUANTITY = 99;

    private static final Sort HISTORY_SORT =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PrepTimeCalculator prepTimeCalculator;
    private final OrdersProperties ordersProperties;
    private final Clock clock;

    @Transactional
    public Order createFromPoster(
            long posterCheckId, Instant paidAt, String comment, List<OrderItem> items) {
        Instant now = clock.instant();
        validateIntake(posterCheckId, paidAt, comment, items, now);
        var order = new Order();
        order.setPosterCheckId(posterCheckId);
        order.setPaidAt(paidAt);
        order.setComment(comment);
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(now);
        order.setEstimatedReadyAt(estimateReadyAt(now, items));
        order.setDisplayNumber(nextDisplayNumber(now));
        order.getItems().addAll(items);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getKitchenQueue() {
        return toResponses(orderRepository.findAllByStatusInOrderByPaidAtAsc(KITCHEN_STATUSES));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getHistory(
            LocalDate date, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, HISTORY_SORT);
        Page<Order> orders;
        if (date == null) {
            orders = status == null
                    ? orderRepository.findAll(pageable)
                    : orderRepository.findAllByStatus(status, pageable);
        } else {
            ZoneId zone = clock.getZone();
            Instant from = date.atStartOfDay(zone).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
            orders = status == null
                    ? orderRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            from, to, pageable)
                    : orderRepository
                            .findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                    status, from, to, pageable);
        }
        return PageResponse.of(orders.map(this::toResponse));
    }

    @Transactional
    public void shiftPrepTime(int deltaMinutes) {
        Duration delta = Duration.ofMinutes(deltaMinutes);
        orderRepository.findAllByStatusInOrderByPaidAtAsc(QUEUE_STATUSES)
                .forEach(order -> order.setEstimatedReadyAt(
                        order.getEstimatedReadyAt().plus(delta)));
    }

    @Transactional
    public OrderResponse startCooking(Long id) {
        Order order = findOrder(id);
        order.setStatus(order.getStatus().startCooking());
        order.setStartedAt(clock.instant());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse markReady(Long id) {
        Order order = findOrder(id);
        order.setStatus(order.getStatus().markReady());
        order.setReadyAt(clock.instant());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse issue(Long id) {
        Order order = findOrder(id);
        order.setStatus(order.getStatus().issue());
        order.setIssuedAt(clock.instant());
        return toResponse(order);
    }

    @Transactional
    public void cancel(Long id) {
        Order order = findOrder(id);
        order.setStatus(order.getStatus().cancel());
        order.setCancelledAt(clock.instant());
    }

    @Transactional
    public OrderResponse restore(Long id) {
        Order order = findOrder(id);
        order.setStatus(order.getStatus().restore());
        order.setReadyAt(null);
        order.setCancelledAt(null);
        if (order.getStartedAt() == null) {
            order.setStartedAt(clock.instant());
        }
        return toResponse(order);
    }

    private void validateIntake(
            long posterCheckId,
            Instant paidAt,
            String comment,
            List<OrderItem> items,
            Instant now) {
        if (posterCheckId <= 0) {
            throw new BadRequestException("posterCheckId must be positive");
        }
        if (paidAt.isAfter(now.plus(ordersProperties.paidAtMaxSkew()))) {
            throw new BadRequestException("paidAt must not be in the future");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new BadRequestException(
                    "comment must be at most %d characters".formatted(MAX_COMMENT_LENGTH));
        }
        if (items.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }
        items.forEach(OrderService::validateItem);
    }

    private static void validateItem(OrderItem item) {
        String name = item.getProductName();
        if (name == null || name.isBlank() || name.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException(
                    "Product name must be 1 to %d characters".formatted(MAX_TEXT_LENGTH));
        }
        if (item.getQuantity() < 1 || item.getQuantity() > MAX_QUANTITY) {
            throw new BadRequestException(
                    "Quantity of '%s' must be between 1 and %d".formatted(name, MAX_QUANTITY));
        }
        if (item.getModifiers().stream().anyMatch(m -> m.length() > MAX_TEXT_LENGTH)) {
            throw new BadRequestException(
                    "Modifiers of '%s' must be at most %d characters"
                            .formatted(name, MAX_TEXT_LENGTH));
        }
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order %d not found".formatted(id)));
    }

    private int nextDisplayNumber(Instant now) {
        ZoneId zone = clock.getZone();
        Instant startOfDay = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant();
        return orderRepository
                .findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(startOfDay)
                .map(Order::getDisplayNumber)
                .orElse(0) + 1;
    }

    private Instant estimateReadyAt(Instant now, List<OrderItem> items) {
        Instant queueEnd = orderRepository.findLatestEstimatedReadyAt(QUEUE_STATUSES)
                .filter(latest -> latest.isAfter(now))
                .orElse(now);
        return queueEnd.plus(prepTimeCalculator.ownTime(items));
    }

    private OrderResponse toResponse(Order order) {
        Instant estimatedReadyAt = FINISHED_STATUSES.contains(order.getStatus())
                ? null
                : order.getEstimatedReadyAt();
        return orderMapper.toResponse(order, estimatedReadyAt);
    }

    private List<OrderResponse> toResponses(List<Order> orders) {
        return orders.stream().map(this::toResponse).toList();
    }
}
