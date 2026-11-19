package com.mishanthrope.queueboard.core.domain.order;

import java.time.Duration;

public record KitchenSettings(
        Duration overhead, Duration wrapPerItem, Duration grill, int grillCapacity) {

    public KitchenSettings {
        requirePositive(overhead, "overhead");
        requirePositive(wrapPerItem, "wrapPerItem");
        requirePositive(grill, "grill");
        if (grillCapacity < 1) {
            throw new IllegalArgumentException("grillCapacity must be at least 1");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be a positive duration");
        }
    }
}
