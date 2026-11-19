package com.mishanthrope.queueboard.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTimingTest {

    private static final Instant T0 = Instant.parse("2026-11-10T08:00:00Z");

    private static Instant at(int minutes) {
        return T0.plus(Duration.ofMinutes(minutes));
    }

    private OrderTiming timing() {
        return new OrderTiming(1L, 7);
    }

    @Test
    void newTiming_hasNoDurations() {
        var timing = timing();

        assertThat(timing.cookingTime()).isEmpty();
        assertThat(timing.waitTime()).isEmpty();
        assertThat(timing.estimateError()).isEmpty();
    }

    @Test
    void start_keepsTheEarliestStartWhateverTheOrderOfArrival() {
        var timing = timing();

        timing.start(at(9));
        timing.start(at(2));
        timing.start(at(5));

        assertThat(timing.getStartedAt()).isEqualTo(at(2));
    }

    @Test
    void complete_keepsTheLatestReadyTimeWhateverTheOrderOfArrival() {
        var timing = timing();

        timing.complete(at(20));
        timing.complete(at(12));
        timing.complete(at(15));

        assertThat(timing.getReadyAt()).isEqualTo(at(20));
    }

    @Test
    void sameFactsInAnyOrder_giveTheSameTiming() {
        var forward = timing();
        forward.start(at(2));
        forward.start(at(9));
        forward.complete(at(12));
        forward.complete(at(20));
        var backward = timing();
        backward.complete(at(20));
        backward.complete(at(12));
        backward.start(at(9));
        backward.start(at(2));

        assertThat(backward.getStartedAt()).isEqualTo(forward.getStartedAt());
        assertThat(backward.getReadyAt()).isEqualTo(forward.getReadyAt());
    }

    @Test
    void durations_areMeasuredFromTheirOwnStartPoints() {
        var timing = timing();
        timing.registerPromise(at(0), at(15));
        timing.start(at(2));
        timing.complete(at(14));

        assertThat(timing.cookingTime()).contains(Duration.ofMinutes(12));
        assertThat(timing.waitTime()).contains(Duration.ofMinutes(14));
        assertThat(timing.estimateError()).contains(Duration.ofMinutes(-1));
    }

    @Test
    void estimateError_isPositiveWhenReadyLaterThanPromised() {
        var timing = timing();
        timing.registerPromise(at(0), at(10));
        timing.complete(at(16));

        assertThat(timing.estimateError()).contains(Duration.ofMinutes(6));
    }

    @Test
    void durations_needBothEnds() {
        var timing = timing();
        timing.registerPromise(at(0), at(15));
        timing.complete(at(14));

        assertThat(timing.cookingTime()).isEmpty();
        assertThat(timing.waitTime()).isPresent();
    }

    @Test
    void reconstitute_restoresEveryField() {
        var timing = OrderTiming.reconstitute(3L, 4, at(0), at(15), at(2), at(14));

        assertThat(timing.getOrderId()).isEqualTo(3L);
        assertThat(timing.getDisplayNumber()).isEqualTo(4);
        assertThat(timing.getPaidAt()).isEqualTo(at(0));
        assertThat(timing.getPromisedAt()).isEqualTo(at(15));
        assertThat(timing.getStartedAt()).isEqualTo(at(2));
        assertThat(timing.getReadyAt()).isEqualTo(at(14));
    }
}
