package com.mishanthrope.queueboard.core.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PrepTimeCalculatorTest {

    private static final int DEFAULT_CAPACITY = 5;

    private static PrepTimeCalculator calculator(int grillCapacity) {
        return new PrepTimeCalculator(new KitchenSettings(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5),
                grillCapacity));
    }

    @ParameterizedTest(name = "{0} items -> {1} min")
    @CsvSource({"1,15", "2,18", "5,27", "6,32", "10,42"})
    void ownTime_followsGrillSimulation(int itemCount, long expectedMinutes) {
        Duration time = calculator(DEFAULT_CAPACITY).ownTime(itemCount);

        assertThat(time).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }

    @ParameterizedTest(name = "{0} items, capacity 3 -> {1} min")
    @CsvSource({"1,15", "3,21", "4,26", "7,35"})
    void ownTime_withSmallerGrill_batchesByCapacity(int itemCount, long expectedMinutes) {
        Duration time = calculator(3).ownTime(itemCount);

        assertThat(time).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }

    @ParameterizedTest(name = "{0} items -> overhead only")
    @CsvSource({"0", "-1"})
    void ownTime_withoutItems_isOverheadOnly(int itemCount) {
        assertThat(calculator(DEFAULT_CAPACITY).ownTime(itemCount))
                .isEqualTo(Duration.ofMinutes(7));
    }
}
