package com.mishanthrope.queueboard.presentation.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PosterWebhookRequest(
        String account,
        String object,
        @JsonProperty("object_id") Long objectId,
        String action,
        String time,
        String verify,
        String data) {
}
