package com.mishanthrope.queueboard.analytics.application.stats;

import com.mishanthrope.queueboard.analytics.api.DailyStats;
import com.mishanthrope.queueboard.analytics.api.StatsApi;
import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import com.mishanthrope.queueboard.analytics.domain.KitchenActionRepository;
import com.mishanthrope.queueboard.analytics.domain.ModifierUsageRepository;
import com.mishanthrope.queueboard.analytics.domain.OrderTiming;
import com.mishanthrope.queueboard.analytics.domain.OrderTimingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyStatsService implements StatsApi {

    private static final int TOP_MODIFIERS = 10;
    private static final double MILLIS_PER_MINUTE = 60_000.0;

    private final KitchenActionRepository actions;
    private final OrderTimingRepository timings;
    private final ModifierUsageRepository modifierUsage;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DailyStats dailyStats(LocalDate day) {
        var from = day.atStartOfDay(clock.getZone()).toInstant();
        var to = day.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

        var cookActions = new LinkedHashMap<String, Long>();
        for (var action : List.of(KitchenAction.STARTED, KitchenAction.COMPLETED,
                KitchenAction.HANDED_OUT, KitchenAction.CANCELLED, KitchenAction.REOPENED)) {
            cookActions.put(publicName(action), actions.count(action, from, to));
        }
        var ready = timings.findReadyBetween(from, to);
        var top = modifierUsage.top(day, TOP_MODIFIERS).stream()
                .map(tally -> new DailyStats.ModifierCount(tally.modifier(), tally.times()))
                .toList();
        return new DailyStats(
                actions.count(KitchenAction.RECEIVED, from, to),
                cookActions.get("ISSUED"),
                cookActions.get("CANCELLED"),
                averageMinutes(ready, OrderTiming::cookingTime),
                averageMinutes(ready, OrderTiming::waitTime),
                averageMinutes(ready, OrderTiming::estimateError),
                cookActions,
                top);
    }

    /** The names used in the public JSON, kept from the first version of the endpoint. */
    private static String publicName(KitchenAction action) {
        return switch (action) {
            case RECEIVED -> "ACCEPTED";
            case STARTED -> "COOKING_STARTED";
            case COMPLETED -> "READY";
            case HANDED_OUT -> "ISSUED";
            case CANCELLED -> "CANCELLED";
            case REOPENED -> "RESTORED";
        };
    }

    private static Double averageMinutes(
            List<OrderTiming> timings, Function<OrderTiming, Optional<Duration>> measure) {
        return timings.stream()
                .map(measure)
                .flatMap(Optional::stream)
                .mapToDouble(duration -> duration.toMillis() / MILLIS_PER_MINUTE)
                .average()
                .stream()
                .mapToObj(value -> Math.round(value * 10) / 10.0)
                .findFirst()
                .orElse(null);
    }
}
