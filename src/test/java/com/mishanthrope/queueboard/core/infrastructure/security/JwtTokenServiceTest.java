package com.mishanthrope.queueboard.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnop";
    private static final Duration EXPIRATION = Duration.ofHours(8);
    private static final Instant ISSUED_AT = Instant.parse("2026-11-18T10:00:00Z");
    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private final JwtTokenService tokenService = serviceAt(ISSUED_AT, SECRET);

    private static JwtTokenService serviceAt(Instant now, String secret) {
        return new JwtTokenService(secret, EXPIRATION, Clock.fixed(now, KYIV));
    }

    @Test
    void verify_forFreshToken_returnsEmail() {
        String token = tokenService.issue("cook@example.com");

        assertThat(tokenService.verify(token)).contains("cook@example.com");
    }

    @Test
    void verify_beforeExpiration_returnsEmail() {
        String token = tokenService.issue("cook@example.com");
        JwtTokenService later = serviceAt(ISSUED_AT.plus(EXPIRATION).minusSeconds(1), SECRET);

        assertThat(later.verify(token)).contains("cook@example.com");
    }

    @Test
    void verify_afterExpiration_returnsEmpty() {
        String token = tokenService.issue("cook@example.com");
        JwtTokenService later = serviceAt(ISSUED_AT.plus(EXPIRATION).plusSeconds(1), SECRET);

        assertThat(later.verify(token)).isEmpty();
    }

    @Test
    void verify_forTokenSignedWithAnotherSecret_returnsEmpty() {
        String forged = serviceAt(ISSUED_AT, "another-secret-0123456789-abcdefghijklmnopqrstu")
                .issue("cook@example.com");

        assertThat(tokenService.verify(forged)).isEmpty();
    }

    @Test
    void verify_forTokenWithTamperedPayload_returnsEmpty() {
        String[] parts = tokenService.issue("cook@example.com").split("\\.");
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@example.com\",\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8));

        String tampered = parts[0] + "." + payload + "." + parts[2];

        assertThat(tokenService.verify(tampered)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "garbage", "a.b.c", "Bearer abc", "...."})
    void verify_forGarbage_returnsEmpty(String token) {
        assertThat(tokenService.verify(token)).isEmpty();
    }
}
