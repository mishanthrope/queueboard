package com.mishanthrope.queueboard.application.order.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.application.order.event.OrderEventPublisher;
import com.mishanthrope.queueboard.application.order.event.OrderReady;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkReadyHandler implements CommandHandler<MarkReady> {

    private final OrderRepository orderRepository;
    private final Clock clock;
    private final OrderEventPublisher events;

    @Override
    @Transactional
    public void handle(MarkReady command) {
        Order order = orderRepository.getById(command.orderId());
        Instant now = clock.instant();
        order.markReady(now);
        orderRepository.save(order);
        events.publish(new OrderReady(
                UUID.randomUUID(), now, order.getId(), order.getDisplayNumber(), now));
    }
}
