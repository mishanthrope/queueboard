package com.mishanthrope.queueboard.application.board.query;

import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Instant;

public record BoardRow(int displayNumber, OrderStatus status, Instant estimatedReadyAt) {
}
