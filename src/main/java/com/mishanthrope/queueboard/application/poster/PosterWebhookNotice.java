package com.mishanthrope.queueboard.application.poster;

public record PosterWebhookNotice(
        String account,
        String object,
        Long objectId,
        String action,
        String time,
        String verify,
        String data) {
}
