package com.mishanthrope.queueboard.application.order.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.util.List;
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
    }
}
