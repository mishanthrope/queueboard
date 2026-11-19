package com.mishanthrope.queueboard.core.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KitchenSettingsTest {

    private static final Duration MIN = Duration.ofMinutes(1);

    static Stream<Arguments> invalidSettings() {
        return Stream.of(
                Arguments.of("null overhead", null, MIN, MIN, 5),
                Arguments.of("zero overhead", Duration.ZERO, MIN, MIN, 5),
                Arguments.of("negative overhead", MIN.negated(), MIN, MIN, 5),
                Arguments.of("null wrap", MIN, null, MIN, 5),
                Arguments.of("zero wrap", MIN, Duration.ZERO, MIN, 5),
                Arguments.of("null grill", MIN, MIN, null, 5),
                Arguments.of("negative grill", MIN, MIN, MIN.negated(), 5),
                Arguments.of("zero capacity", MIN, MIN, MIN, 0),
                Arguments.of("negative capacity", MIN, MIN, MIN, -1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSettings")
    void constructor_withInvalidValue_throws(
            String description, Duration overhead, Duration wrap, Duration grill, int capacity) {
        assertThatThrownBy(() -> new KitchenSettings(overhead, wrap, grill, capacity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_withPositiveValues_keepsThem() {
        var settings = new KitchenSettings(MIN, MIN.multipliedBy(2), MIN.multipliedBy(3), 1);

        assertThat(settings.overhead()).isEqualTo(MIN);
        assertThat(settings.wrapPerItem()).isEqualTo(MIN.multipliedBy(2));
        assertThat(settings.grill()).isEqualTo(MIN.multipliedBy(3));
        assertThat(settings.grillCapacity()).isEqualTo(1);
    }
}
