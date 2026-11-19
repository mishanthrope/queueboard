package com.mishanthrope.queueboard.core.application.order.query;

import com.mishanthrope.queueboard.core.application.common.QueryHandler;
import com.mishanthrope.queueboard.core.domain.order.OrderNotFoundError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderDetailsHandler implements QueryHandler<GetOrderDetails, OrderView> {

    private final OrderReadRepository orderReadRepository;

    @Override
    public OrderView handle(GetOrderDetails query) {
        return orderReadRepository.findById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundError(query.orderId()));
    }
}
