package com.mishanthrope.queueboard.application.order.query;

import com.mishanthrope.queueboard.application.common.PageView;
import com.mishanthrope.queueboard.application.common.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderHistoryHandler
        implements QueryHandler<GetOrderHistory, PageView<OrderView>> {

    private final OrderReadRepository orderReadRepository;

    @Override
    public PageView<OrderView> handle(GetOrderHistory query) {
        return orderReadRepository.findHistory(
                query.date(), query.status(), query.page(), query.size());
    }
}
