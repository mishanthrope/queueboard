package com.mishanthrope.queueboard.core.application.order.query;

import com.mishanthrope.queueboard.core.application.common.QueryHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetKitchenQueueHandler implements QueryHandler<GetKitchenQueue, List<OrderView>> {

    private final OrderReadRepository orderReadRepository;

    @Override
    public List<OrderView> handle(GetKitchenQueue query) {
        return orderReadRepository.findActive();
    }
}
