package com.mishanthrope.queueboard.core.application.board.query;

import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.Instant;

public record BoardRow(int displayNumber, OrderStatus status, Instant estimatedReadyAt) {
}
