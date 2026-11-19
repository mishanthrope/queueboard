package com.mishanthrope.queueboard.analytics.domain;

import java.time.Instant;
import java.util.UUID;

/** Log of kitchen actions; its primary key makes processing idempotent. */
public interface KitchenActionRepository {

    /** Records the action under the fact id; false if this fact id was recorded before. */
    boolean registerIfNew(UUID factId, KitchenAction action, long orderId, Instant at);

    long count(KitchenAction action, Instant from, Instant to);
}
