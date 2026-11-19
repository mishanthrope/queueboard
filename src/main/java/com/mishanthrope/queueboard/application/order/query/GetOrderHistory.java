package com.mishanthrope.queueboard.application.order.query;

import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.LocalDate;

public record GetOrderHistory(LocalDate date, OrderStatus status, int page, int size) {
}
