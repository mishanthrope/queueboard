package com.mishanthrope.queueboard.core.application.poster;

import com.mishanthrope.queueboard.core.application.poster.command.ProcessPosterWebhook;

public interface WebhookSignatureVerifier {

    boolean isValid(ProcessPosterWebhook command);
}
