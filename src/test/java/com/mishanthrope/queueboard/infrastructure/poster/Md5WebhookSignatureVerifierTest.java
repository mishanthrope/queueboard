package com.mishanthrope.queueboard.infrastructure.poster;

import static org.assertj.core.api.Assertions.assertThat;

import com.mishanthrope.queueboard.application.poster.command.ProcessPosterWebhook;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Md5WebhookSignatureVerifierTest {

    private static final String SECRET = "local-dev-poster-secret";
    private static final String KNOWN_SIGNATURE = "a1a76179b60cdc30c584824ae98635db";

    private final PosterProperties properties = new PosterProperties(SECRET);
    private final Md5WebhookSignatureVerifier verifier =
            new Md5WebhookSignatureVerifier(properties);
    private final SignedWebhooks webhooks = new SignedWebhooks(properties);

    private static String md5(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static ProcessPosterWebhook notice(
            String account, String object, Long objectId, String action, String time,
            String verify, String data) {
        return new ProcessPosterWebhook(account, object, objectId, action, time, verify, data);
    }

    static Stream<Arguments> unsignedWebhooks() {
        return Stream.of(
                Arguments.of("wrong signature",
                        notice("shawarma", "transaction", 1L, "changed", "10", "deadbeef", null)),
                Arguments.of("missing verify",
                        notice("shawarma", "transaction", 1L, "changed", "10", null, null)),
                Arguments.of("missing account",
                        notice(null, "transaction", 1L, "changed", "10", "abc", null)),
                Arguments.of("missing object",
                        notice("shawarma", null, 1L, "changed", "10", "abc", null)),
                Arguments.of("missing object id",
                        notice("shawarma", "transaction", null, "changed", "10", "abc", null)),
                Arguments.of("missing action",
                        notice("shawarma", "transaction", 1L, null, "10", "abc", null)),
                Arguments.of("missing time",
                        notice("shawarma", "transaction", 1L, "changed", null, "abc", null)));
    }

    @Test
    void isValid_withValidSignature_isTrue() {
        assertThat(verifier.isValid(webhooks.webhook("transaction", 48213, "changed"))).isTrue();
    }

    @Test
    void isValid_matchesKnownSignatureFromApiDocs() {
        ProcessPosterWebhook notice =
                notice("shawarma", "transaction", 48213L, "changed", "1794919500",
                KNOWN_SIGNATURE, null);

        assertThat(verifier.isValid(notice)).isTrue();
    }

    @Test
    void isValid_withData_includesDataInSignedString() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;{\"a\":1};10;" + SECRET);
        ProcessPosterWebhook notice =
                notice("shawarma", "transaction", 7L, "changed", "10", signature,
                "{\"a\":1}");

        assertThat(verifier.isValid(notice)).isTrue();
    }

    @Test
    void isValid_withDataSignedWithoutData_isFalse() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;10;" + SECRET);
        ProcessPosterWebhook notice =
                notice("shawarma", "transaction", 7L, "changed", "10", signature,
                "{\"a\":1}");

        assertThat(verifier.isValid(notice)).isFalse();
    }

    @Test
    void isValid_withEmptyData_ignoresData() throws Exception {
        String signature = md5("shawarma;transaction;7;changed;10;" + SECRET);
        ProcessPosterWebhook notice =
                notice("shawarma", "transaction", 7L, "changed", "10", signature, "");

        assertThat(verifier.isValid(notice)).isTrue();
    }

    @Test
    void isValid_withUpperCaseHex_isTrue() {
        ProcessPosterWebhook valid = webhooks.webhook("transaction", 5, "changed");
        ProcessPosterWebhook upper =
                notice(valid.account(), valid.object(), valid.objectId(), valid.action(),
                valid.time(), valid.verify().toUpperCase(Locale.ROOT), null);

        assertThat(verifier.isValid(upper)).isTrue();
    }

    @Test
    void isValid_withSignatureFromAnotherSecret_isFalse() {
        var other = new SignedWebhooks(new PosterProperties("another-secret"));

        assertThat(verifier.isValid(other.webhook("transaction", 5, "changed"))).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsignedWebhooks")
    void isValid_withBadWebhook_isFalse(String description, ProcessPosterWebhook notice) {
        assertThat(verifier.isValid(notice)).isFalse();
    }
}
