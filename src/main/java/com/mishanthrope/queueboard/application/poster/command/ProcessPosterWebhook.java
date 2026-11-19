package com.mishanthrope.queueboard.application.poster.command;

public record ProcessPosterWebhook(
        String account,
        String object,
        Long objectId,
        String action,
        String time,
        String verify,
        String data) {
}
