package com.mishanthrope.queueboard.application.poster;

public interface WebhookSignatureVerifier {

    boolean isValid(PosterWebhookNotice notice);
}
