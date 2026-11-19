package com.mishanthrope.queueboard.application.order.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftPrepTimeHandler implements CommandHandler<ShiftPrepTime> {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void handle(ShiftPrepTime command) {
        Duration delta = Duration.ofMinutes(command.deltaMinutes());
        for (var order : orderRepository.findInQueue()) {
            order.shiftEstimate(delta);
            orderRepository.save(order);
        }
    }
}
