package com.mishanthrope.queueboard.application.order.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.application.order.event.OrderAccepted;
import com.mishanthrope.queueboard.application.order.event.OrderEventPublisher;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderFromPosterHandler implements CommandHandler<CreateOrderFromPoster> {

    private final OrderRepository orderRepository;
    private final OrderFactory orderFactory;
    private final Clock clock;
    private final OrderEventPublisher events;

    @Override
    @Transactional
    public void handle(CreateOrderFromPoster command) {
        List<OrderItem> items = command.lines().stream()
                .map(line -> new OrderItem(
                        null, line.productName(), line.quantity(), line.modifiers()))
                .toList();
        Order order = orderRepository.save(orderFactory.createFromPoster(
                command.posterCheckId(), command.paidAt(), command.comment(), items));
        log.info("Poster check {} accepted as order {} (display number {})",
                command.posterCheckId(), order.getId(), order.getDisplayNumber());
        events.publish(new OrderAccepted(
                UUID.randomUUID(),
                clock.instant(),
                order.getId(),
                order.getDisplayNumber(),
                order.getPosterCheckId(),
                order.getPaidAt(),
                order.getEstimatedReadyAt(),
                order.getItems().stream()
                        .map(item -> new OrderAccepted.Item(
                                item.productName(), item.quantity(), item.modifiers()))
                        .toList()));
    }
}
