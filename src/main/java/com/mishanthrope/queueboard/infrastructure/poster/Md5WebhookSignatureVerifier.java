package com.mishanthrope.queueboard.infrastructure.poster;

import com.mishanthrope.queueboard.application.poster.PosterWebhookNotice;
import com.mishanthrope.queueboard.application.poster.WebhookSignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Md5WebhookSignatureVerifier implements WebhookSignatureVerifier {

    private final PosterProperties properties;

    @Override
    public boolean isValid(PosterWebhookNotice notice) {
        if (notice.verify() == null || !hasSignedFields(notice)) {
            return false;
        }
        byte[] expected = expectedSignature(notice).getBytes(StandardCharsets.UTF_8);
        byte[] provided = notice.verify().strip().toLowerCase(Locale.ROOT)
                .getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private static boolean hasSignedFields(PosterWebhookNotice notice) {
        return notice.account() != null
                && notice.object() != null
                && notice.objectId() != null
                && notice.action() != null
                && notice.time() != null;
    }

    private String expectedSignature(PosterWebhookNotice notice) {
        List<String> parts = new ArrayList<>(List.of(
                notice.account(),
                notice.object(),
                String.valueOf(notice.objectId()),
                notice.action()));
        if (notice.data() != null && !notice.data().isEmpty()) {
            parts.add(notice.data());
        }
        parts.add(notice.time());
        parts.add(properties.applicationSecret());
        return md5Hex(String.join(";", parts));
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md5.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
