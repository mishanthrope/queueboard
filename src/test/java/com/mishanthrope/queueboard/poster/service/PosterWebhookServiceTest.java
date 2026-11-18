package com.mishanthrope.queueboard.poster.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.common.exception.BadRequestException;
import com.mishanthrope.queueboard.order.exception.IllegalStatusTransitionException;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderItem;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import com.mishanthrope.queueboard.order.service.OrderService;
import com.mishanthrope.queueboard.poster.client.PosterClient;
import com.mishanthrope.queueboard.poster.dto.PosterTransaction;
import com.mishanthrope.queueboard.poster.dto.PosterTransactionProduct;
import com.mishanthrope.queueboard.poster.dto.PosterWebhook;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PosterWebhookServiceTest {

    private static final long CHECK_ID = 5L;
    private static final long CLOSED_AT_MILLIS = 1_794_999_038_000L;

    @Mock
    private PosterClient posterClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PosterWebhookService service;

    private static PosterWebhook webhook(String object, String action) {
        return new PosterWebhook("shawarma", object, CHECK_ID, action, "10", "sig", null);
    }

    private static PosterTransaction transaction(
            String status, String payType, String dateClose, String comment) {
        return new PosterTransaction("5", status, payType, dateClose, comment);
    }

    private static PosterTransaction paidTransaction() {
        return transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), null);
    }

    private static Order order(long id, OrderStatus status) {
        var order = new Order();
        order.setId(id);
        order.setStatus(status);
        return order;
    }

    static Stream<Arguments> notPaidTransactions() {
        return Stream.of(
                Arguments.of("not closed", transaction("1", "1", "1", null)),
                Arguments.of("closed without payment", transaction("2", "0", "1", null)),
                Arguments.of("no pay type", transaction("2", null, "1", null)),
                Arguments.of("no transaction at all", null));
    }

    @Test
    void handle_forObjectOtherThanTransaction_doesNothing() {
        service.handle(webhook("product", "changed"));

        verifyNoInteractions(posterClient, orderRepository, orderService);
    }

    @Test
    void handle_removedForExistingOrder_cancelsIt() {
        when(orderRepository.findByPosterCheckId(CHECK_ID))
                .thenReturn(Optional.of(order(10L, OrderStatus.NEW)));

        service.handle(webhook("transaction", "removed"));

        verify(orderService).cancel(10L);
    }

    @Test
    void handle_removedForIssuedOrder_swallowsTransitionError() {
        when(orderRepository.findByPosterCheckId(CHECK_ID))
                .thenReturn(Optional.of(order(10L, OrderStatus.ISSUED)));
        doThrow(new IllegalStatusTransitionException("cancel", OrderStatus.ISSUED))
                .when(orderService).cancel(10L);

        assertThatCode(() -> service.handle(webhook("transaction", "removed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_removedForUnknownCheck_doesNothing() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> service.handle(webhook("transaction", "removed")))
                .doesNotThrowAnyException();

        verify(orderService, never()).cancel(anyLong());
        verifyNoInteractions(posterClient);
    }

    @Test
    void handle_whenOrderAlreadyExists_doesNotCallPosterApi() {
        when(orderRepository.findByPosterCheckId(CHECK_ID))
                .thenReturn(Optional.of(order(10L, OrderStatus.NEW)));

        service.handle(webhook("transaction", "changed"));

        verifyNoInteractions(posterClient, orderService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("notPaidTransactions")
    void handle_whenCheckIsNotClosedAndPaid_createsNoOrder(
            String description, PosterTransaction transaction) {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(transaction);

        service.handle(webhook("transaction", "changed"));

        verify(posterClient, never()).getTransactionProducts(anyLong());
        verifyNoInteractions(orderService);
    }

    @Test
    void handle_whenCheckIsClosedAndPaid_passesParsedValuesToOrderService() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(
                transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), "з собою"));
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(List.of(
                new PosterTransactionProduct("Шаурма", "2", "без цибулі, гострий соус ,"),
                new PosterTransactionProduct("Кола", "3.000", null)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenReturn(order(1L, OrderStatus.NEW));

        service.handle(webhook("transaction", "changed"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> items = ArgumentCaptor.forClass(List.class);
        verify(orderService).createFromPoster(
                eq(CHECK_ID), eq(Instant.ofEpochMilli(CLOSED_AT_MILLIS)), eq("з собою"),
                items.capture());
        assertThat(items.getValue()).extracting(OrderItem::getProductName)
                .containsExactly("Шаурма", "Кола");
        assertThat(items.getValue()).extracting(OrderItem::getQuantity)
                .containsExactly(2, 3);
        assertThat(items.getValue().get(0).getModifiers())
                .containsExactly("без цибулі", "гострий соус");
        assertThat(items.getValue().get(1).getModifiers()).isEmpty();
    }

    @Test
    void handle_withBlankComment_passesNullComment() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(
                transaction("2", "1", String.valueOf(CLOSED_AT_MILLIS), "   "));
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(
                List.of(new PosterTransactionProduct("Шаурма", "1", null)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenReturn(order(1L, OrderStatus.NEW));

        service.handle(webhook("transaction", "changed"));

        verify(orderService).createFromPoster(
                eq(CHECK_ID), any(Instant.class), eq(null), anyList());
    }

    @Test
    void handle_whenOrderServiceRejectsData_doesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(paidTransaction());
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(
                List.of(new PosterTransactionProduct("Шаурма", "1", null)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new BadRequestException("invalid"));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenOrderWasCreatedConcurrently_doesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(paidTransaction());
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(
                List.of(new PosterTransactionProduct("Шаурма", "1", null)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenPosterClientFails_doesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenThrow(new IllegalStateException("down"));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_whenProductsCannotBeFetched_doesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(paidTransaction());
        when(posterClient.getTransactionProducts(CHECK_ID))
                .thenThrow(new IllegalStateException("down"));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_withoutCloseDate_createsNoOrderAndDoesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID))
                .thenReturn(transaction("2", "1", null, null));
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(
                List.of(new PosterTransactionProduct("Шаурма", "1", null)));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_withNonNumericQuantity_createsNoOrderAndDoesNotThrow() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());
        when(posterClient.getTransaction(CHECK_ID)).thenReturn(paidTransaction());
        when(posterClient.getTransactionProducts(CHECK_ID)).thenReturn(
                List.of(new PosterTransactionProduct("Шаурма", "abc", null)));

        assertThatCode(() -> service.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }
}
