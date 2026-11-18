package com.mishanthrope.queueboard.poster.service;

import com.mishanthrope.queueboard.common.exception.BadRequestException;
import com.mishanthrope.queueboard.order.exception.IllegalStatusTransitionException;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderItem;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import com.mishanthrope.queueboard.order.service.OrderService;
import com.mishanthrope.queueboard.poster.client.PosterClient;
import com.mishanthrope.queueboard.poster.dto.PosterTransaction;
import com.mishanthrope.queueboard.poster.dto.PosterTransactionProduct;
import com.mishanthrope.queueboard.poster.dto.PosterWebhook;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterWebhookService {

    private static final String OBJECT_TRANSACTION = "transaction";
    private static final String ACTION_REMOVED = "removed";
    private static final String STATUS_CLOSED = "2";
    private static final String PAY_TYPE_NOT_PAID = "0";

    private final PosterClient posterClient;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public void handle(PosterWebhook webhook) {
        if (!OBJECT_TRANSACTION.equals(webhook.object())) {
            log.debug("Ignoring Poster webhook for object '{}'", webhook.object());
            return;
        }
        long transactionId = webhook.objectId();
        try {
            if (ACTION_REMOVED.equals(webhook.action())) {
                cancelOrder(transactionId);
            } else {
                importOrder(transactionId);
            }
        } catch (RuntimeException e) {
            log.error("Failed to process Poster webhook for transaction {}", transactionId, e);
        }
    }

    private void cancelOrder(long transactionId) {
        Order order = orderRepository.findByPosterCheckId(transactionId).orElse(null);
        if (order == null) {
            log.info("Poster check {} was removed, but there is no such order", transactionId);
            return;
        }
        try {
            orderService.cancel(order.getId());
            log.info("Poster check {} was removed, order {} cancelled", transactionId,
                    order.getId());
        } catch (IllegalStatusTransitionException e) {
            log.info("Poster check {} was removed, order not cancelled: {}", transactionId,
                    e.getMessage());
        }
    }

    private void importOrder(long transactionId) {
        if (orderRepository.findByPosterCheckId(transactionId).isPresent()) {
            log.debug("Order for Poster check {} already exists, ignoring", transactionId);
            return;
        }
        PosterTransaction transaction = posterClient.getTransaction(transactionId);
        if (!isClosedAndPaid(transaction)) {
            log.debug("Poster check {} is not closed and paid, ignoring", transactionId);
            return;
        }
        List<PosterTransactionProduct> products = posterClient.getTransactionProducts(
                transactionId);
        try {
            Order order = orderService.createFromPoster(
                    transactionId,
                    parsePaidAt(transaction.dateClose()),
                    blankToNull(transaction.transactionComment()),
                    toItems(products));
            log.info("Poster check {} accepted as order {} (display number {})",
                    transactionId, order.getId(), order.getDisplayNumber());
        } catch (BadRequestException e) {
            log.warn("Skipping Poster check {}: {}", transactionId, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.info("Poster check {} was accepted concurrently, ignoring", transactionId);
        }
    }

    private static boolean isClosedAndPaid(PosterTransaction transaction) {
        return transaction != null
                && STATUS_CLOSED.equals(transaction.status())
                && transaction.payType() != null
                && !PAY_TYPE_NOT_PAID.equals(transaction.payType());
    }

    private static Instant parsePaidAt(String dateClose) {
        if (dateClose == null) {
            throw new BadRequestException("Poster check has no date_close");
        }
        try {
            long millis = Long.parseLong(dateClose.strip());
            if (millis <= 0) {
                throw new BadRequestException("Invalid date_close: " + dateClose);
            }
            return Instant.ofEpochMilli(millis);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid date_close: " + dateClose);
        }
    }

    private static List<OrderItem> toItems(List<PosterTransactionProduct> products) {
        var items = new ArrayList<OrderItem>();
        if (products == null) {
            return items;
        }
        for (PosterTransactionProduct product : products) {
            var item = new OrderItem();
            item.setProductName(product.productName());
            item.setQuantity(parseQuantity(product.num()));
            item.setModifiers(splitModifiers(product.modificatorName()));
            items.add(item);
        }
        return items;
    }

    private static int parseQuantity(String num) {
        if (num == null) {
            throw new BadRequestException("Poster product has no num");
        }
        try {
            return new BigDecimal(num.strip()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new BadRequestException("Invalid product quantity: " + num);
        }
    }

    private static List<String> splitModifiers(String modificatorName) {
        if (modificatorName == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(modificatorName.split(","))
                .map(String::strip)
                .filter(modifier -> !modifier.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
