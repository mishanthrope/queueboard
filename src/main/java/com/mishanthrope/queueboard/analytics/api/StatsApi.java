package com.mishanthrope.queueboard.analytics.api;

import java.time.LocalDate;

/** Daily kitchen statistics, as the rest of the system sees them. */
public interface StatsApi {

    /** Statistics for the given day in the application's time zone (half-open day). */
    DailyStats dailyStats(LocalDate day);
}
