package com.mishanthrope.queueboard.analytics.domain;

import java.time.LocalDate;
import java.util.List;

public interface ModifierUsageRepository {

    void add(LocalDate day, ModifierTally tally);

    /** Most used modifiers of the day, most frequent first. */
    List<ModifierTally> top(LocalDate day, int limit);
}
