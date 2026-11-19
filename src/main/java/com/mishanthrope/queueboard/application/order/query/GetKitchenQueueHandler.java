package com.mishanthrope.queueboard.application.order.query;

import com.mishanthrope.queueboard.application.common.QueryHandler;
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
