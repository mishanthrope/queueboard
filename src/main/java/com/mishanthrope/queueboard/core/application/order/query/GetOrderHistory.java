package com.mishanthrope.queueboard.core.application.order.query;

import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.LocalDate;

public record GetOrderHistory(LocalDate date, OrderStatus status, int page, int size) {
}
