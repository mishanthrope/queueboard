package com.mishanthrope.queueboard.application.poster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.application.order.OrderService;
import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.IllegalStatusTransitionError;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PosterWebhookServiceTest {

    private static final long CHECK_ID = 5L;
    private static final Instant PAID_AT = Instant.ofEpochMilli(1_794_999_038_000L);

    @Mock
    private WebhookSignatureVerifier signatureVerifier;

    @Mock
    private PosterClient posterClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PosterWebhookService service;

    @BeforeEach
    void setUp() {
        lenient().when(signatureVerifier.isValid(any())).thenReturn(true);
    }

    private static PosterWebhookNotice notice(String object, String action) {
        return new PosterWebhookNotice("shawarma", object, CHECK_ID, action, "10", "sig", null);
    }

    private static PosterCheckLine line(String name, int quantity, String... modifiers) {
        return new PosterCheckLine(name, quantity, List.of(modifiers));
    }

    private static PosterCheck check(String comment, PosterCheckLine... lines) {
        return new PosterCheck(PAID_AT, comment, List.of(lines));
    }

    private static Order order(long id, OrderStatus status) {
        return Order.reconstitute(
                id, CHECK_ID, 1, status, null, PAID_AT, PAID_AT, PAID_AT, null, null, null,
                null, List.of());
    }

    private void givenPaidCheck(PosterCheck check) {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID)).thenReturn(Optional.of(check));
    }

    @Test
    void handle_withInvalidSignature_throwsAndTouchesNothing() {
        when(signatureVerifier.isValid(any())).thenReturn(false);

        assertThatThrownBy(() -> service.handle(notice("transaction", "changed")))
                .isInstanceOf(InvalidWebhookSignatureError.class)
                .hasMessage("Missing or invalid Poster signature");

        verifyNoInteractions(posterClient, orderRepository, orderService);
    }

    @Test
    void handle_forObjectOtherThanTransaction_doesNothing() {
        service.handle(notice("product", "changed"));

        verifyNoInteractions(posterClient, orderRepository, orderService);
    }

    @Test
    void handle_removedForExistingOrder_cancelsIt() {
        when(orderRepository.findByPosterCheckId(CHECK_ID))
                .thenReturn(Optional.of(order(10L, OrderStatus.NEW)));

        service.handle(notice("transaction", "removed"));

        verify(orderService).cancel(10L);
    }

    @Test
    void handle_removedForIssuedOrder_swallowsTransitionError() {
        when(orderRepository.findByPosterCheckId(CHECK_ID))
                .thenReturn(Optional.of(order(10L, OrderStatus.ISSUED)));
        doThrow(new IllegalStatusTransitionError("cancel", OrderStatus.ISSUED))
                .when(orderService).cancel(10L);

        assertThatCode(() -> service.handle(notice("transaction", "removed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_removedForUnknownCheck_doesNothing() {
        when(orderRepository.findByPosterCheckId(CHECK_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> service.handle(notice("transaction", "removed")))
                .doesNotThrowAnyException();

        verify(orderService, never()).cancel(anyLong());
        verifyNoInteractions(posterClient);
    }

    @Test
    void handle_whenOrderAlreadyExists_doesNotCallPoster() {
        when(orderRepository.existsByPosterCheckId(CHECK_ID)).thenReturn(true);

        service.handle(notice("transaction", "changed"));

        verifyNoInteractions(posterClient, orderService);
    }

    @Test
    void handle_whenCheckIsNotClosedAndPaid_createsNoOrder() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID)).thenReturn(Optional.empty());

        service.handle(notice("transaction", "changed"));

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_whenCheckIsClosedAndPaid_passesCheckValuesToOrderService() {
        givenPaidCheck(check("з собою",
                line("Шаурма", 2, "без цибулі", "гострий соус"),
                line("Кола", 3)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenReturn(order(1L, OrderStatus.NEW));

        service.handle(notice("transaction", "changed"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> items = ArgumentCaptor.forClass(List.class);
        verify(orderService).createFromPoster(
                eq(CHECK_ID), eq(PAID_AT), eq("з собою"), items.capture());
        assertThat(items.getValue()).extracting(OrderItem::productName)
                .containsExactly("Шаурма", "Кола");
        assertThat(items.getValue()).extracting(OrderItem::quantity).containsExactly(2, 3);
        assertThat(items.getValue().get(0).modifiers())
                .containsExactly("без цибулі", "гострий соус");
        assertThat(items.getValue().get(1).modifiers()).isEmpty();
        assertThat(items.getValue()).extracting(OrderItem::id).containsOnlyNulls();
    }

    @Test
    void handle_whenOrderServiceRejectsData_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new InvalidOrderError("invalid"));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenOrderWasCreatedConcurrently_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenDomainReportsDuplicateCheck_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new DuplicatePosterCheckError(CHECK_ID));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenPosterClientFails_doesNotThrow() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID))
                .thenThrow(new IllegalStateException("down"));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_whenPosterDataIsMalformed_createsNoOrderAndDoesNotThrow() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID))
                .thenThrow(new PosterDataError("Poster check has no date_close"));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_withLineThatBreaksItemRules_createsNoOrderAndDoesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 0)));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(orderService);
    }

    @Test
    void handle_withCheckWithoutLines_passesEmptyItemsToOrderService() {
        givenPaidCheck(check(null));
        when(orderService.createFromPoster(anyLong(), any(), any(), anyList()))
                .thenThrow(new InvalidOrderError("Order must contain at least one item"));

        assertThatCode(() -> service.handle(notice("transaction", "changed")))
                .doesNotThrowAnyException();

        verify(orderService).createFromPoster(eq(CHECK_ID), eq(PAID_AT), eq(null), eq(List.of()));
    }
}
