package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

import com.mishanthrope.queueboard.analytics.domain.ModifierTally;
import com.mishanthrope.queueboard.analytics.domain.ModifierUsageRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ModifierUsageRepositoryAdapter implements ModifierUsageRepository {

    private final SpringDataModifierUsageRepository usage;

    @Override
    public void add(LocalDate day, ModifierTally tally) {
        usage.increment(day, tally.modifier(), tally.times());
    }

    @Override
    public List<ModifierTally> top(LocalDate day, int limit) {
        return usage.findByKeyDayOrderByTimesDescKeyModifierAsc(day, PageRequest.of(0, limit))
                .stream()
                .map(entity -> new ModifierTally(entity.getKey().modifier(), entity.getTimes()))
                .toList();
    }
}
