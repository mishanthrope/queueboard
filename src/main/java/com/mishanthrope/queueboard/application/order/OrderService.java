package com.mishanthrope.queueboard.application.order;

import com.mishanthrope.queueboard.domain.common.PageResult;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderNotFoundError;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderFactory orderFactory;
    private final Clock clock;

    @Transactional
    public Order createFromPoster(
            long posterCheckId, Instant paidAt, String comment, List<OrderItem> items) {
        return orderRepository.save(
                orderFactory.createFromPoster(posterCheckId, paidAt, comment, items));
    }

    @Transactional(readOnly = true)
    public List<Order> getKitchenQueue() {
        return orderRepository.findActive();
    }

    @Transactional(readOnly = true)
    public Order getOrder(long id) {
        return findOrder(id);
    }

    @Transactional(readOnly = true)
    public PageResult<Order> getHistory(LocalDate date, OrderStatus status, int page, int size) {
        return orderRepository.findHistory(date, status, page, size);
    }

    @Transactional
    public void shiftPrepTime(int deltaMinutes) {
        Duration delta = Duration.ofMinutes(deltaMinutes);
        for (Order order : orderRepository.findInQueue()) {
            order.shiftEstimate(delta);
            orderRepository.save(order);
        }
    }

    @Transactional
    public Order startCooking(long id) {
        Order order = findOrder(id);
        order.startCooking(clock.instant());
        return orderRepository.save(order);
    }

    @Transactional
    public Order markReady(long id) {
        Order order = findOrder(id);
        order.markReady(clock.instant());
        return orderRepository.save(order);
    }

    @Transactional
    public Order issue(long id) {
        Order order = findOrder(id);
        order.issue(clock.instant());
        return orderRepository.save(order);
    }

    @Transactional
    public void cancel(long id) {
        Order order = findOrder(id);
        order.cancel(clock.instant());
        orderRepository.save(order);
    }

    @Transactional
    public Order restore(long id) {
        Order order = findOrder(id);
        order.restore(clock.instant());
        return orderRepository.save(order);
    }

    private Order findOrder(long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundError(id));
    }
}
