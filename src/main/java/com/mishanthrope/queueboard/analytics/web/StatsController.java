package com.mishanthrope.queueboard.analytics.web;

import com.mishanthrope.queueboard.analytics.internal.StatsService;
import com.mishanthrope.queueboard.analytics.internal.StatsView;
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

    private final StatsService stats;

    @GetMapping
    public StatsView getStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return stats.forDay(date);
    }
}
