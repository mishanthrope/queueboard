package com.mishanthrope.queueboard.analytics.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataModifierUsageRepository
        extends JpaRepository<ModifierUsageEntity, ModifierUsageEntity.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO analytics_modifier_usage (day, modifier, times)
            VALUES (:day, :modifier, :times)
            ON CONFLICT (day, modifier)
            DO UPDATE SET times = analytics_modifier_usage.times + EXCLUDED.times""",
            nativeQuery = true)
    void increment(
            @Param("day") LocalDate day,
            @Param("modifier") String modifier,
            @Param("times") long times);

    List<ModifierUsageEntity> findByKeyDayOrderByTimesDescKeyModifierAsc(
            LocalDate day, Pageable pageable);
}
