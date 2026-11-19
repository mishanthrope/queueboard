package com.mishanthrope.queueboard.domain.order;

import java.util.List;

public record OrderItem(Long id, String productName, int quantity, List<String> modifiers) {

    public static final int MAX_TEXT_LENGTH = 100;
    public static final int MAX_QUANTITY = 99;

    public OrderItem {
        if (productName == null
                || productName.isBlank()
                || productName.length() > MAX_TEXT_LENGTH) {
            throw new InvalidOrderError(
                    "Product name must be 1 to %d characters".formatted(MAX_TEXT_LENGTH));
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidOrderError(
                    "Quantity of '%s' must be between 1 and %d"
                            .formatted(productName, MAX_QUANTITY));
        }
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        if (modifiers.stream().anyMatch(m -> m.length() > MAX_TEXT_LENGTH)) {
            throw new InvalidOrderError(
                    "Modifiers of '%s' must be at most %d characters"
                            .formatted(productName, MAX_TEXT_LENGTH));
        }
    }
}
