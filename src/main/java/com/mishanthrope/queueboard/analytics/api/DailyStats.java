package com.mishanthrope.queueboard.analytics.api;

import java.util.List;
import java.util.Map;

/**
 * Statistics of one day. Averages are in minutes with one decimal, {@code null} when there is
 * nothing to average. {@code cookActions} is keyed by the public names COOKING_STARTED, READY,
 * ISSUED, CANCELLED, RESTORED.
 */
public record DailyStats(
        long ordersAccepted,
        long ordersIssued,
        long ordersCancelled,
        Double averageCookingMinutes,
        Double averageWaitMinutes,
        Double averageEstimateErrorMinutes,
        Map<String, Long> cookActions,
        List<ModifierCount> topModifiers) {

    public record ModifierCount(String modifier, long times) {
    }
}
