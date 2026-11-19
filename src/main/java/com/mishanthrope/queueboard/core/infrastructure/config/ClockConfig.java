package com.mishanthrope.queueboard.core.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    private static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
