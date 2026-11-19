package com.mishanthrope.queueboard.infrastructure.poster.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PosterTransactionProduct(
        @JsonProperty("product_name") String productName,
        String num,
        @JsonProperty("modificator_name") String modificatorName) {
}
