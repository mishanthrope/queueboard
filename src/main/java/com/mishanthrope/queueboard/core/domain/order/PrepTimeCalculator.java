package com.mishanthrope.queueboard.core.domain.order;

import java.time.Duration;

public class PrepTimeCalculator {

    private final KitchenSettings settings;

    public PrepTimeCalculator(KitchenSettings settings) {
        this.settings = settings;
    }

    public Duration ownTime(int itemCount) {
        int remaining = itemCount;
        Duration wrappedAt = Duration.ZERO;
        Duration grillFreeAt = Duration.ZERO;
        while (remaining > 0) {
            int batch = Math.min(settings.grillCapacity(), remaining);
            wrappedAt = wrappedAt.plus(settings.wrapPerItem().multipliedBy(batch));
            Duration start = wrappedAt.compareTo(grillFreeAt) >= 0 ? wrappedAt : grillFreeAt;
            grillFreeAt = start.plus(settings.grill());
            remaining -= batch;
        }
        return settings.overhead().plus(grillFreeAt);
    }
}
