package com.mishanthrope.queueboard.poster.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.poster")
public record PosterProperties(@NotBlank String applicationSecret) {
}
