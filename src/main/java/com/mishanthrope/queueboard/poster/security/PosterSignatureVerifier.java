package com.mishanthrope.queueboard.poster.security;

import com.mishanthrope.queueboard.common.exception.UnauthorizedException;
import com.mishanthrope.queueboard.poster.config.PosterProperties;
import com.mishanthrope.queueboard.poster.dto.PosterWebhook;
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
public class PosterSignatureVerifier {

    private final PosterProperties properties;

    public void verify(PosterWebhook webhook) {
        if (webhook.verify() == null || !hasSignedFields(webhook)) {
            throw new UnauthorizedException("Missing or invalid Poster signature");
        }
        byte[] expected = expectedSignature(webhook).getBytes(StandardCharsets.UTF_8);
        byte[] provided = webhook.verify().strip().toLowerCase(Locale.ROOT)
                .getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new UnauthorizedException("Missing or invalid Poster signature");
        }
    }

    private static boolean hasSignedFields(PosterWebhook webhook) {
        return webhook.account() != null
                && webhook.object() != null
                && webhook.objectId() != null
                && webhook.action() != null
                && webhook.time() != null;
    }

    private String expectedSignature(PosterWebhook webhook) {
        List<String> parts = new ArrayList<>(List.of(
                webhook.account(),
                webhook.object(),
                String.valueOf(webhook.objectId()),
                webhook.action()));
        if (webhook.data() != null && !webhook.data().isEmpty()) {
            parts.add(webhook.data());
        }
        parts.add(webhook.time());
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
