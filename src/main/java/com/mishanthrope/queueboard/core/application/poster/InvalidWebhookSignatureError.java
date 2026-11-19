package com.mishanthrope.queueboard.core.application.poster;

import com.mishanthrope.queueboard.core.application.common.ApplicationError;

public class InvalidWebhookSignatureError extends ApplicationError {

    public InvalidWebhookSignatureError() {
        super("Missing or invalid Poster signature");
    }
}
