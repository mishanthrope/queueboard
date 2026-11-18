package com.mishanthrope.queueboard.common.security;

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

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnop";
    private static final Duration EXPIRATION = Duration.ofHours(8);
    private static final Instant ISSUED_AT = Instant.parse("2026-11-18T10:00:00Z");
    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private final JwtService jwtService = serviceAt(ISSUED_AT, SECRET);

    private static JwtService serviceAt(Instant now, String secret) {
        return new JwtService(secret, EXPIRATION, Clock.fixed(now, KYIV));
    }

    @Test
    void extractEmail_forFreshToken_returnsEmail() {
        String token = jwtService.generateToken("cook@example.com");

        assertThat(jwtService.extractEmail(token)).contains("cook@example.com");
    }

    @Test
    void extractEmail_beforeExpiration_returnsEmail() {
        String token = jwtService.generateToken("cook@example.com");
        JwtService later = serviceAt(ISSUED_AT.plus(EXPIRATION).minusSeconds(1), SECRET);

        assertThat(later.extractEmail(token)).contains("cook@example.com");
    }

    @Test
    void extractEmail_afterExpiration_returnsEmpty() {
        String token = jwtService.generateToken("cook@example.com");
        JwtService later = serviceAt(ISSUED_AT.plus(EXPIRATION).plusSeconds(1), SECRET);

        assertThat(later.extractEmail(token)).isEmpty();
    }

    @Test
    void extractEmail_forTokenSignedWithAnotherSecret_returnsEmpty() {
        String forged = serviceAt(ISSUED_AT, "another-secret-0123456789-abcdefghijklmnopqrstu")
                .generateToken("cook@example.com");

        assertThat(jwtService.extractEmail(forged)).isEmpty();
    }

    @Test
    void extractEmail_forTokenWithTamperedPayload_returnsEmpty() {
        String[] parts = jwtService.generateToken("cook@example.com").split("\\.");
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@example.com\",\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8));

        String tampered = parts[0] + "." + payload + "." + parts[2];

        assertThat(jwtService.extractEmail(tampered)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "garbage", "a.b.c", "Bearer abc", "...."})
    void extractEmail_forGarbage_returnsEmpty(String token) {
        assertThat(jwtService.extractEmail(token)).isEmpty();
    }
}
