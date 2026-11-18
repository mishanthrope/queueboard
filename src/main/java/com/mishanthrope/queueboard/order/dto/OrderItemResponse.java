package com.mishanthrope.queueboard.order.dto;

import java.util.List;

public record OrderItemResponse(
        Long id,
        String productName,
        int quantity,
        List<String> modifiers) {
}
