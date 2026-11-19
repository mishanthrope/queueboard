package com.mishanthrope.queueboard.core.infrastructure.poster;

import com.mishanthrope.queueboard.core.application.poster.WebhookSignatureVerifier;
import com.mishanthrope.queueboard.core.application.poster.command.ProcessPosterWebhook;
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
    public boolean isValid(ProcessPosterWebhook command) {
        if (command.verify() == null || !hasSignedFields(command)) {
            return false;
        }
        byte[] expected = expectedSignature(command).getBytes(StandardCharsets.UTF_8);
        byte[] provided = command.verify().strip().toLowerCase(Locale.ROOT)
                .getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private static boolean hasSignedFields(ProcessPosterWebhook command) {
        return command.account() != null
                && command.object() != null
                && command.objectId() != null
                && command.action() != null
                && command.time() != null;
    }

    private String expectedSignature(ProcessPosterWebhook command) {
        List<String> parts = new ArrayList<>(List.of(
                command.account(),
                command.object(),
                String.valueOf(command.objectId()),
                command.action()));
        if (command.data() != null && !command.data().isEmpty()) {
            parts.add(command.data());
        }
        parts.add(command.time());
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
