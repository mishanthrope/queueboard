package com.mishanthrope.queueboard.application.poster;

import com.mishanthrope.queueboard.application.common.ApplicationError;

public class InvalidWebhookSignatureError extends ApplicationError {

    public InvalidWebhookSignatureError() {
        super("Missing or invalid Poster signature");
    }
}
