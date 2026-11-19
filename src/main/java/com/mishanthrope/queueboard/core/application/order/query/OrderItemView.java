package com.mishanthrope.queueboard.core.application.order.query;

import java.util.List;

public record OrderItemView(Long id, String productName, int quantity, List<String> modifiers) {
}
