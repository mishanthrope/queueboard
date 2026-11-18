package com.mishanthrope.queueboard.poster.controller;

import com.mishanthrope.queueboard.poster.dto.PosterWebhook;
import com.mishanthrope.queueboard.poster.security.PosterSignatureVerifier;
import com.mishanthrope.queueboard.poster.service.PosterWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PosterWebhookController {

    private final PosterSignatureVerifier signatureVerifier;
    private final PosterWebhookService webhookService;

    @PostMapping("/poster")
    public void receivePosterWebhook(@RequestBody PosterWebhook webhook) {
        signatureVerifier.verify(webhook);
        webhookService.handle(webhook);
    }
}
