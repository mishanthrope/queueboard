package com.mishanthrope.queueboard.application.poster.command;

import com.mishanthrope.queueboard.application.common.CommandHandler;
import com.mishanthrope.queueboard.application.order.command.CancelOrder;
import com.mishanthrope.queueboard.application.order.command.CancelOrderHandler;
import com.mishanthrope.queueboard.application.order.command.CreateOrderFromPoster;
import com.mishanthrope.queueboard.application.order.command.CreateOrderFromPosterHandler;
import com.mishanthrope.queueboard.application.poster.InvalidWebhookSignatureError;
import com.mishanthrope.queueboard.application.poster.PosterCheck;
import com.mishanthrope.queueboard.application.poster.PosterCheckLine;
import com.mishanthrope.queueboard.application.poster.PosterClient;
import com.mishanthrope.queueboard.application.poster.PosterDataError;
import com.mishanthrope.queueboard.application.poster.WebhookSignatureVerifier;
import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.IllegalStatusTransitionError;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.Order;
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
public class ProcessPosterWebhookHandler implements CommandHandler<ProcessPosterWebhook> {

    private static final String OBJECT_TRANSACTION = "transaction";
    private static final String ACTION_REMOVED = "removed";

    private final WebhookSignatureVerifier signatureVerifier;
    private final PosterClient posterClient;
    private final OrderRepository orderRepository;
    private final CancelOrderHandler cancelOrderHandler;
    private final CreateOrderFromPosterHandler createOrderFromPosterHandler;

    @Override
    public void handle(ProcessPosterWebhook command) {
        if (!signatureVerifier.isValid(command)) {
            throw new InvalidWebhookSignatureError();
        }
        if (!OBJECT_TRANSACTION.equals(command.object())) {
            log.debug("Ignoring Poster webhook for object '{}'", command.object());
            return;
        }
        long transactionId = command.objectId();
        try {
            if (ACTION_REMOVED.equals(command.action())) {
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
            cancelOrderHandler.handle(new CancelOrder(order.getId()));
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
            createOrderFromPosterHandler.handle(new CreateOrderFromPoster(
                    transactionId,
                    check.get().paidAt(),
                    check.get().comment(),
                    toLines(check.get().lines())));
        } catch (InvalidOrderError | PosterDataError e) {
            log.warn("Skipping Poster check {}: {}", transactionId, e.getMessage());
        } catch (DuplicatePosterCheckError | DataIntegrityViolationException e) {
            log.info("Poster check {} was accepted concurrently, ignoring", transactionId);
        }
    }

    private static List<CreateOrderFromPoster.Line> toLines(List<PosterCheckLine> lines) {
        return lines.stream()
                .map(line -> new CreateOrderFromPoster.Line(
                        line.productName(), line.quantity(), line.modifiers()))
                .toList();
    }
}
