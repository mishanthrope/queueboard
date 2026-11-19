package com.mishanthrope.queueboard.core.presentation.order;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShiftPrepTimeRequest(@NotNull @Min(-30) @Max(30) Integer deltaMinutes) {

    @AssertTrue(message = "must not be zero")
    public boolean isDeltaMinutesNonZero() {
        return deltaMinutes == null || deltaMinutes != 0;
    }
}
