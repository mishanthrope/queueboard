package com.mishanthrope.queueboard.poster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PosterWebhook(
        String account,
        String object,
        @JsonProperty("object_id") Long objectId,
        String action,
        String time,
        String verify,
        String data) {
}
