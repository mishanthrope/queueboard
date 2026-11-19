package com.mishanthrope.queueboard.core.application.order.query;

import com.mishanthrope.queueboard.core.application.common.PageView;
import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderReadRepository {

    List<OrderView> findActive();

    Optional<OrderView> findById(long id);

    PageView<OrderView> findHistory(LocalDate date, OrderStatus status, int page, int size);
}
