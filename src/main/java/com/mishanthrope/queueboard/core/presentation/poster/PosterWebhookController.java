package com.mishanthrope.queueboard.core.presentation.poster;

import com.mishanthrope.queueboard.core.application.poster.command.ProcessPosterWebhookHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PosterWebhookController {

    private final ProcessPosterWebhookHandler processPosterWebhook;
    private final PosterWebhookMapper mapper;

    @PostMapping("/poster")
    public void receivePosterWebhook(@RequestBody PosterWebhookRequest request) {
        processPosterWebhook.handle(mapper.toCommand(request));
    }
}
