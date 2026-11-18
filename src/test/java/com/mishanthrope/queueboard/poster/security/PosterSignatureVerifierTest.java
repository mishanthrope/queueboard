package com.mishanthrope.queueboard.poster.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mishanthrope.queueboard.common.exception.UnauthorizedException;
import com.mishanthrope.queueboard.poster.SignedWebhooks;
import com.mishanthrope.queueboard.poster.config.PosterProperties;
import com.mishanthrope.queueboard.poster.dto.PosterWebhook;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PosterSignatureVerifierTest {

    private static final String SECRET = "local-dev-poster-secret";
    private static final String KNOWN_SIGNATURE = "a1a76179b60cdc30c584824ae98635db";

    private final PosterProperties properties = new PosterProperties(SECRET);
    private final PosterSignatureVerifier verifier = new PosterSignatureVerifier(properties);
    private final SignedWebhooks webhooks = new SignedWebhooks(properties);

    private static String md5(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static PosterWebhook webhook(
            String account, String object, Long objectId, String action, String time,
            String verify, String data) {
        return new PosterWebhook(account, object, objectId, action, time, verify, data);
    }

    static Stream<Arguments> unsignedWebhooks() {
        return Stream.of(
                Arguments.of("wrong signature",
                        webhook("shawarma", "transaction", 1L, "changed", "10", "deadbeef", null)),
                Arguments.of("missing verify",
                        webhook("shawarma", "transaction", 1L, "changed", "10", null, null)),
                Arguments.of("missing account",
                        webhook(null, "transaction", 1L, "changed", "10", "abc", null)),
                Arguments.of("missing object",
                        webhook("shawarma", null, 1L, "changed", "10", "abc", null)),
                Arguments.of("missing object id",
                        webhook("shawarma", "transaction", null, "changed", "10", "abc", null)),
                Arguments.of("missing action",
                        webhook("shawarma", "transaction", 1L, null, "10", "abc", null)),
                Arguments.of("missing time",
                        webhook("shawarma", "transaction", 1L, "changed", null, "abc", null)));
    }

    @Test
    void verify_withValidSignature_passes() {
        assertThatCode(() -> verifier.verify(webhooks.webhook("transaction", 48213, "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void verify_matchesKnownSignatureFromApiDocs() {
        var webhook = webhook("shawarma", "transaction", 48213L, "changed", "1794919500",
                KNOWN_SIGNATURE, null);

        assertThatCode(() -> verifier.verify(webhook)).doesNotThrowAnyException();
    }

    @Test
    void verify_withData_includesDataInSignedString() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;{\"a\":1};10;" + SECRET);
        var webhook = webhook("shawarma", "transaction", 7L, "changed", "10", signature,
                "{\"a\":1}");

        assertThatCode(() -> verifier.verify(webhook)).doesNotThrowAnyException();
    }

    @Test
    void verify_withDataSignedWithoutData_throwsUnauthorized() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;10;" + SECRET);
        var webhook = webhook("shawarma", "transaction", 7L, "changed", "10", signature,
                "{\"a\":1}");

        assertThatThrownBy(() -> verifier.verify(webhook))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_withEmptyData_ignoresData() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;10;" + SECRET);
        var webhook = webhook("shawarma", "transaction", 7L, "changed", "10", signature, "");

        assertThatCode(() -> verifier.verify(webhook)).doesNotThrowAnyException();
    }

    @Test
    void verify_withUpperCaseHex_passes() {
        PosterWebhook valid = webhooks.webhook("transaction", 5, "changed");
        var upper = webhook(valid.account(), valid.object(), valid.objectId(), valid.action(),
                valid.time(), valid.verify().toUpperCase(Locale.ROOT), null);

        assertThatCode(() -> verifier.verify(upper)).doesNotThrowAnyException();
    }

    @Test
    void verify_withSignatureFromAnotherSecret_throwsUnauthorized() {
        var other = new SignedWebhooks(new PosterProperties("another-secret"));

        assertThatThrownBy(() -> verifier.verify(other.webhook("transaction", 5, "changed")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsignedWebhooks")
    void verify_withBadWebhook_throwsUnauthorized(String description, PosterWebhook webhook) {
        assertThatThrownBy(() -> verifier.verify(webhook))
                .isInstanceOf(UnauthorizedException.class);
    }
}
