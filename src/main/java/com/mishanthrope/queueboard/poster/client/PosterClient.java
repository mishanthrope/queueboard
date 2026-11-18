package com.mishanthrope.queueboard.poster.client;

import com.mishanthrope.queueboard.poster.dto.PosterTransaction;
import com.mishanthrope.queueboard.poster.dto.PosterTransactionProduct;
import java.util.List;

public interface PosterClient {

    PosterTransaction getTransaction(long transactionId);

    List<PosterTransactionProduct> getTransactionProducts(long transactionId);
}
