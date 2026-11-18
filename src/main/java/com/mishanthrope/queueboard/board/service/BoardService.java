package com.mishanthrope.queueboard.board.service;

import com.mishanthrope.queueboard.board.dto.BoardResponse;
import com.mishanthrope.queueboard.board.dto.PreparingEntry;
import com.mishanthrope.queueboard.board.dto.ReadyEntry;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import com.mishanthrope.queueboard.order.service.OrderService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final long MILLIS_PER_MINUTE = Duration.ofMinutes(1).toMillis();

    private final OrderRepository orderRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BoardResponse getBoard() {
        Instant now = clock.instant();
        var preparing = new ArrayList<PreparingEntry>();
        var ready = new ArrayList<ReadyEntry>();
        for (Order order : orderRepository.findAllByStatusInOrderByPaidAtAsc(
                OrderService.KITCHEN_STATUSES)) {
            if (order.getStatus() == OrderStatus.READY) {
                ready.add(new ReadyEntry(order.getDisplayNumber()));
            } else {
                preparing.add(new PreparingEntry(
                        order.getDisplayNumber(),
                        minutesLeft(now, order.getEstimatedReadyAt())));
            }
        }
        return new BoardResponse(preparing, ready);
    }

    private static long minutesLeft(Instant now, Instant estimatedReadyAt) {
        long millisLeft = Duration.between(now, estimatedReadyAt).toMillis();
        return millisLeft <= 0 ? 0 : Math.ceilDiv(millisLeft, MILLIS_PER_MINUTE);
    }
}
