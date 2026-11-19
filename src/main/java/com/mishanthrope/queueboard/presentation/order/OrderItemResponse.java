package com.mishanthrope.queueboard.presentation.order;

import java.util.List;

public record OrderItemResponse(
        Long id,
        String productName,
        int quantity,
        List<String> modifiers) {
}
