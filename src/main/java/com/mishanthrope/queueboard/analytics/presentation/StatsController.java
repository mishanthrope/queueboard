package com.mishanthrope.queueboard.analytics.presentation;

import com.mishanthrope.queueboard.analytics.api.DailyStats;
import com.mishanthrope.queueboard.analytics.api.StatsApi;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsApi stats;
    private final Clock clock;

    @GetMapping
    public DailyStats getStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return stats.dailyStats(date == null ? LocalDate.now(clock) : date);
    }
}
