package com.mishanthrope.queueboard.application.poster;

import com.mishanthrope.queueboard.application.poster.command.ProcessPosterWebhook;

public interface WebhookSignatureVerifier {

    boolean isValid(ProcessPosterWebhook command);
}
