package com.mishanthrope.queueboard.application.order.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderHandler implements CommandHandler<CancelOrder> {

    private final OrderRepository orderRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(CancelOrder command) {
        Order order = orderRepository.getById(command.orderId());
        order.cancel(clock.instant());
        orderRepository.save(order);
    }
}
