package com.mishanthrope.queueboard.application.order.command;

import java.time.Instant;
import java.util.List;

public record CreateOrderFromPoster(
        long posterCheckId, Instant paidAt, String comment, List<Line> lines) {

    public record Line(String productName, int quantity, List<String> modifiers) {
    }
}
