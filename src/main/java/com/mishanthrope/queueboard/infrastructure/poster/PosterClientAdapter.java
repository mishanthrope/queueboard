package com.mishanthrope.queueboard.infrastructure.poster;

import com.mishanthrope.queueboard.application.poster.PosterCheck;
import com.mishanthrope.queueboard.application.poster.PosterCheckLine;
import com.mishanthrope.queueboard.application.poster.PosterClient;
import com.mishanthrope.queueboard.application.poster.PosterDataError;
import com.mishanthrope.queueboard.infrastructure.poster.api.PosterApi;
import com.mishanthrope.queueboard.infrastructure.poster.api.dto.PosterTransaction;
import com.mishanthrope.queueboard.infrastructure.poster.api.dto.PosterTransactionProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosterClientAdapter implements PosterClient {

    private static final String STATUS_CLOSED = "2";
    private static final String PAY_TYPE_NOT_PAID = "0";

    private final PosterApi posterApi;

    @Override
    public Optional<PosterCheck> findClosedAndPaidCheck(long checkId) {
        PosterTransaction transaction = posterApi.getTransaction(checkId);
        if (!isClosedAndPaid(transaction)) {
            return Optional.empty();
        }
        List<PosterTransactionProduct> products = posterApi.getTransactionProducts(checkId);
        return Optional.of(new PosterCheck(
                parsePaidAt(transaction.dateClose()),
                blankToNull(transaction.transactionComment()),
                toLines(products)));
    }

    private static boolean isClosedAndPaid(PosterTransaction transaction) {
        return transaction != null
                && STATUS_CLOSED.equals(transaction.status())
                && transaction.payType() != null
                && !PAY_TYPE_NOT_PAID.equals(transaction.payType());
    }

    private static Instant parsePaidAt(String dateClose) {
        if (dateClose == null) {
            throw new PosterDataError("Poster check has no date_close");
        }
        try {
            long millis = Long.parseLong(dateClose.strip());
            if (millis <= 0) {
                throw new PosterDataError("Invalid date_close: " + dateClose);
            }
            return Instant.ofEpochMilli(millis);
        } catch (NumberFormatException e) {
            throw new PosterDataError("Invalid date_close: " + dateClose);
        }
    }

    private static List<PosterCheckLine> toLines(List<PosterTransactionProduct> products) {
        if (products == null) {
            return List.of();
        }
        return products.stream()
                .map(product -> new PosterCheckLine(
                        product.productName(),
                        parseQuantity(product.num()),
                        splitModifiers(product.modificatorName())))
                .toList();
    }

    private static int parseQuantity(String num) {
        if (num == null) {
            throw new PosterDataError("Poster product has no num");
        }
        try {
            return new BigDecimal(num.strip()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new PosterDataError("Invalid product quantity: " + num);
        }
    }

    private static List<String> splitModifiers(String modificatorName) {
        if (modificatorName == null) {
            return List.of();
        }
        return Arrays.stream(modificatorName.split(","))
                .map(String::strip)
                .filter(modifier -> !modifier.isEmpty())
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
