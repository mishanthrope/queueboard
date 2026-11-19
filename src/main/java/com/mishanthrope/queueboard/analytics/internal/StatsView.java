package com.mishanthrope.queueboard.analytics.internal;

import java.util.List;
import java.util.Map;

public record StatsView(
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
