package com.mishanthrope.queueboard.core.infrastructure.poster.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PosterTransaction(
        @JsonProperty("transaction_id") String transactionId,
        String status,
        @JsonProperty("pay_type") String payType,
        @JsonProperty("date_close") String dateClose,
        @JsonProperty("transaction_comment") String transactionComment) {
}
