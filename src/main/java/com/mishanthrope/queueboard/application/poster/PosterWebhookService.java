package com.mishanthrope.queueboard.application.poster;

import com.mishanthrope.queueboard.application.order.OrderService;
import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.IllegalStatusTransitionError;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import java.util.List;
import java.util.Optional;
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

    private final WebhookSignatureVerifier signatureVerifier;
    private final PosterClient posterClient;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public void handle(PosterWebhookNotice notice) {
        if (!signatureVerifier.isValid(notice)) {
            throw new InvalidWebhookSignatureError();
        }
        if (!OBJECT_TRANSACTION.equals(notice.object())) {
            log.debug("Ignoring Poster webhook for object '{}'", notice.object());
            return;
        }
        long transactionId = notice.objectId();
        try {
            if (ACTION_REMOVED.equals(notice.action())) {
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
        } catch (IllegalStatusTransitionError e) {
            log.info("Poster check {} was removed, order not cancelled: {}", transactionId,
                    e.getMessage());
        }
    }

    private void importOrder(long transactionId) {
        if (orderRepository.existsByPosterCheckId(transactionId)) {
            log.debug("Order for Poster check {} already exists, ignoring", transactionId);
            return;
        }
        try {
            Optional<PosterCheck> check = posterClient.findClosedAndPaidCheck(transactionId);
            if (check.isEmpty()) {
                log.debug("Poster check {} is not closed and paid, ignoring", transactionId);
                return;
            }
            Order order = orderService.createFromPoster(
                    transactionId,
                    check.get().paidAt(),
                    check.get().comment(),
                    toItems(check.get().lines()));
            log.info("Poster check {} accepted as order {} (display number {})",
                    transactionId, order.getId(), order.getDisplayNumber());
        } catch (InvalidOrderError | PosterDataError e) {
            log.warn("Skipping Poster check {}: {}", transactionId, e.getMessage());
        } catch (DuplicatePosterCheckError | DataIntegrityViolationException e) {
            log.info("Poster check {} was accepted concurrently, ignoring", transactionId);
        }
    }

    private static List<OrderItem> toItems(List<PosterCheckLine> lines) {
        return lines.stream()
                .map(line -> new OrderItem(
                        null, line.productName(), line.quantity(), line.modifiers()))
                .toList();
    }
}
