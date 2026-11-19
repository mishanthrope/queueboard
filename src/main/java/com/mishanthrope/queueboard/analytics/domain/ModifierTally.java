package com.mishanthrope.queueboard.analytics.domain;

/** How many times a modifier was ordered. */
public record ModifierTally(String modifier, long times) {

    public ModifierTally {
        if (modifier == null || modifier.isBlank()) {
            throw new IllegalArgumentException("Modifier must not be blank");
        }
        if (times < 1) {
            throw new IllegalArgumentException("Times must be positive");
        }
    }
}
