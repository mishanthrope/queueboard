package com.mishanthrope.queueboard.infrastructure.poster;

import com.mishanthrope.queueboard.application.poster.command.ProcessPosterWebhook;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SignedWebhooks {

    public static final String ACCOUNT = "shawarma";
    public static final String TIME = "1794919500";

    private final String secret;

    public SignedWebhooks(PosterProperties properties) {
        this.secret = properties.applicationSecret();
    }

    public String signature(String object, long objectId, String action) {
        return md5(String.join(";", ACCOUNT, object, String.valueOf(objectId), action, TIME,
                secret));
    }

    public ProcessPosterWebhook webhook(String object, long objectId, String action) {
        return new ProcessPosterWebhook(ACCOUNT, object, objectId, action, TIME,
                signature(object, objectId, action), null);
    }

    public String json(String object, long objectId, String action) {
        return jsonWithVerify(object, objectId, action, signature(object, objectId, action));
    }

    public String jsonWithVerify(String object, long objectId, String action, String verify) {
        return """
                {"account":"%s","object":"%s","object_id":%d,"action":"%s","time":"%s",\
                "verify":"%s"}"""
                .formatted(ACCOUNT, object, objectId, action, TIME, verify);
    }

    private static String md5(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
