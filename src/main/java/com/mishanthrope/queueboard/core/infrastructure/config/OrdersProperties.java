package com.mishanthrope.queueboard.core.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.orders")
public record OrdersProperties(@NotNull Duration paidAtMaxSkew, @NotNull @Valid PrepTime prepTime) {

    public record PrepTime(
            @NotNull Duration overhead,
            @NotNull Duration wrapPerItem,
            @NotNull Duration grill,
            @Min(1) int grillCapacity) {
    }
}
