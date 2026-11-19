package com.mishanthrope.queueboard.presentation.poster;

import com.mishanthrope.queueboard.application.poster.PosterWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PosterWebhookController {

    private final PosterWebhookService webhookService;
    private final PosterWebhookMapper mapper;

    @PostMapping("/poster")
    public void receivePosterWebhook(@RequestBody PosterWebhookRequest request) {
        webhookService.handle(mapper.toNotice(request));
    }
}
