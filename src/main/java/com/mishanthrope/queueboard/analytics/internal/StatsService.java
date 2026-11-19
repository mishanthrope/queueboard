package com.mishanthrope.queueboard.analytics.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String[] ACTIONS =
            {"COOKING_STARTED", "READY", "ISSUED", "CANCELLED", "RESTORED"};

    private final SpringDataProcessedEventRepository processedEvents;
    private final SpringDataAnalyticsOrderRepository orders;
    private final SpringDataModifierUsageRepository modifierUsage;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StatsView forDay(LocalDate requested) {
        var day = requested == null ? LocalDate.now(clock) : requested;
        var from = day.atStartOfDay(clock.getZone()).toInstant();
        var to = day.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

        var actions = new LinkedHashMap<String, Long>();
        for (var action : ACTIONS) {
            actions.put(action, count(action, from, to));
        }
        var averages = orders.averagesForReadyBetween(from, to);
        var top = modifierUsage.findTop10ByKeyDayOrderByTimesDescKeyModifierAsc(day).stream()
                .map(usage -> new StatsView.ModifierCount(
                        usage.getKey().modifier(), usage.getTimes()))
                .toList();
        return new StatsView(
                count("ACCEPTED", from, to),
                actions.get("ISSUED"),
                actions.get("CANCELLED"),
                oneDecimal(averages.getCooking()),
                oneDecimal(averages.getWait()),
                oneDecimal(averages.getError()),
                actions,
                top);
    }

    private long count(String type, Instant from, Instant to) {
        return processedEvents
                .countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        type, from, to);
    }

    private static Double oneDecimal(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }
}
