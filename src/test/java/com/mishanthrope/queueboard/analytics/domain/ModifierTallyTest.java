package com.mishanthrope.queueboard.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ModifierTallyTest {

    @Test
    void validTally_keepsValues() {
        var tally = new ModifierTally("Гострий", 3);

        assertThat(tally.modifier()).isEqualTo("Гострий");
        assertThat(tally.times()).isEqualTo(3);
    }

    @Test
    void blankModifier_isRejected() {
        assertThatThrownBy(() -> new ModifierTally(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonPositiveTimes_isRejected() {
        assertThatThrownBy(() -> new ModifierTally("Гострий", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
