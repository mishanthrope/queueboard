package com.mishanthrope.queueboard.application.order.query;

import com.mishanthrope.queueboard.application.common.PageView;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderReadRepository {

    List<OrderView> findActive();

    Optional<OrderView> findById(long id);

    PageView<OrderView> findHistory(LocalDate date, OrderStatus status, int page, int size);
}
