package com.mishanthrope.queueboard.core.infrastructure.poster.api;

import com.mishanthrope.queueboard.core.infrastructure.poster.api.dto.PosterTransaction;
import com.mishanthrope.queueboard.core.infrastructure.poster.api.dto.PosterTransactionProduct;
import java.util.List;

public interface PosterApi {

    PosterTransaction getTransaction(long transactionId);

    List<PosterTransactionProduct> getTransactionProducts(long transactionId);
}
