package com.mishanthrope.queueboard.application.poster.command;

import static com.mishanthrope.queueboard.application.order.OrderFixtures.seed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.mishanthrope.queueboard.domain.order.InMemoryOrderRepository;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.KitchenSettings;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import com.mishanthrope.queueboard.domain.order.PrepTimeCalculator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ProcessPosterWebhookHandlerTest {

    private static final long CHECK_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant PAID_AT = NOW.minusSeconds(5);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Europe/Kyiv"));

    @Mock
    private WebhookSignatureVerifier signatureVerifier;

    @Mock
    private PosterClient posterClient;

    @Mock
    private CancelOrderHandler cancelOrderHandler;

    @Mock
    private CreateOrderFromPosterHandler createOrderHandler;

    private InMemoryOrderRepository repository;
    private ProcessPosterWebhookHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        lenient().when(signatureVerifier.isValid(any())).thenReturn(true);
        handler = new ProcessPosterWebhookHandler(
                signatureVerifier, posterClient, repository, cancelOrderHandler,
                createOrderHandler);
    }

    private ProcessPosterWebhookHandler realHandler() {
        var settings = new KitchenSettings(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), 5);
        var factory = new OrderFactory(
                repository, new PrepTimeCalculator(settings), Duration.ofMinutes(2), CLOCK);
        return new ProcessPosterWebhookHandler(
                signatureVerifier, posterClient, repository,
                new CancelOrderHandler(repository, CLOCK, event -> { }),
                new CreateOrderFromPosterHandler(repository, factory, CLOCK, event -> { }));
    }

    private static ProcessPosterWebhook webhook(String object, String action) {
        return new ProcessPosterWebhook("shawarma", object, CHECK_ID, action, "10", "sig", null);
    }

    private static PosterCheckLine line(String name, int quantity, String... modifiers) {
        return new PosterCheckLine(name, quantity, List.of(modifiers));
    }

    private static PosterCheck check(String comment, PosterCheckLine... lines) {
        return new PosterCheck(PAID_AT, comment, List.of(lines));
    }

    private void givenPaidCheck(PosterCheck check) {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID)).thenReturn(Optional.of(check));
    }

    private long seedOrder(OrderStatus status) {
        return seed(repository, CHECK_ID, 1, status, NOW.minusSeconds(60), NOW).getId();
    }

    @Test
    void handle_withInvalidSignature_throwsAndTouchesNothing() {
        when(signatureVerifier.isValid(any())).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(webhook("transaction", "changed")))
                .isInstanceOf(InvalidWebhookSignatureError.class)
                .hasMessage("Missing or invalid Poster signature");

        verifyNoInteractions(posterClient, cancelOrderHandler, createOrderHandler);
    }

    @Test
    void handle_forObjectOtherThanTransaction_doesNothing() {
        handler.handle(webhook("product", "changed"));

        verifyNoInteractions(posterClient, cancelOrderHandler, createOrderHandler);
    }

    @Test
    void handle_removedForExistingOrder_sendsCancelCommand() {
        long id = seedOrder(OrderStatus.NEW);

        handler.handle(webhook("transaction", "removed"));

        verify(cancelOrderHandler).handle(new CancelOrder(id));
    }

    @Test
    void handle_removedForExistingOrder_cancelsItInTheRepository() {
        long id = seedOrder(OrderStatus.NEW);

        realHandler().handle(webhook("transaction", "removed"));

        assertThat(repository.findById(id).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handle_removedForIssuedOrder_swallowsTransitionError() {
        long id = seedOrder(OrderStatus.ISSUED);
        doThrow(new IllegalStatusTransitionError("cancel", OrderStatus.ISSUED))
                .when(cancelOrderHandler).handle(new CancelOrder(id));

        assertThatCode(() -> handler.handle(webhook("transaction", "removed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_removedForIssuedOrder_keepsItIssued() {
        long id = seedOrder(OrderStatus.ISSUED);

        assertThatCode(() -> realHandler().handle(webhook("transaction", "removed")))
                .doesNotThrowAnyException();

        assertThat(repository.findById(id).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.ISSUED);
    }

    @Test
    void handle_removedForUnknownCheck_doesNothing() {
        assertThatCode(() -> handler.handle(webhook("transaction", "removed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(cancelOrderHandler, posterClient);
    }

    @Test
    void handle_whenOrderAlreadyExists_doesNotCallPoster() {
        seedOrder(OrderStatus.NEW);

        handler.handle(webhook("transaction", "changed"));

        verifyNoInteractions(posterClient, createOrderHandler);
    }

    @Test
    void handle_whenCheckIsNotClosedAndPaid_createsNoOrder() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID)).thenReturn(Optional.empty());

        handler.handle(webhook("transaction", "changed"));

        verifyNoInteractions(createOrderHandler);
    }

    @Test
    void handle_whenCheckIsClosedAndPaid_sendsCommandWithCheckValues() {
        givenPaidCheck(check("з собою",
                line("Шаурма", 2, "без цибулі", "гострий соус"),
                line("Кола", 3)));

        handler.handle(webhook("transaction", "changed"));

        ArgumentCaptor<CreateOrderFromPoster> command =
                ArgumentCaptor.forClass(CreateOrderFromPoster.class);
        verify(createOrderHandler).handle(command.capture());
        assertThat(command.getValue().posterCheckId()).isEqualTo(CHECK_ID);
        assertThat(command.getValue().paidAt()).isEqualTo(PAID_AT);
        assertThat(command.getValue().comment()).isEqualTo("з собою");
        List<CreateOrderFromPoster.Line> lines = command.getValue().lines();
        assertThat(lines).extracting(CreateOrderFromPoster.Line::productName)
                .containsExactly("Шаурма", "Кола");
        assertThat(lines).extracting(CreateOrderFromPoster.Line::quantity).containsExactly(2, 3);
        assertThat(lines.get(0).modifiers()).containsExactly("без цибулі", "гострий соус");
        assertThat(lines.get(1).modifiers()).isEmpty();
    }

    @Test
    void handle_whenCheckIsClosedAndPaid_storesTheOrderInTheRepository() {
        givenPaidCheck(check("з собою", line("Шаурма", 2, "сир")));

        realHandler().handle(webhook("transaction", "changed"));

        Order order = repository.findByPosterCheckId(CHECK_ID).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getComment()).isEqualTo("з собою");
        assertThat(order.getItems()).extracting(OrderItem::productName).containsExactly("Шаурма");
    }

    @Test
    void handle_sameWebhookTwice_keepsExactlyOneOrder() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        ProcessPosterWebhookHandler real = realHandler();

        real.handle(webhook("transaction", "changed"));
        real.handle(webhook("transaction", "changed"));

        assertThat(repository.findMaxDisplayNumberCreatedSince(NOW.minus(Duration.ofDays(1))))
                .contains(1);
    }

    @Test
    void handle_whenCommandIsRejectedByDomain_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        doThrow(new InvalidOrderError("invalid")).when(createOrderHandler).handle(any());

        assertThatCode(() -> handler.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenOrderWasCreatedConcurrently_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(createOrderHandler).handle(any());

        assertThatCode(() -> handler.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenDomainReportsDuplicateCheck_doesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 1)));
        doThrow(new DuplicatePosterCheckError(CHECK_ID)).when(createOrderHandler).handle(any());

        assertThatCode(() -> handler.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();
    }

    @Test
    void handle_whenPosterClientFails_doesNotThrow() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID))
                .thenThrow(new IllegalStateException("down"));

        assertThatCode(() -> handler.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(createOrderHandler);
    }

    @Test
    void handle_whenPosterDataIsMalformed_createsNoOrderAndDoesNotThrow() {
        when(posterClient.findClosedAndPaidCheck(CHECK_ID))
                .thenThrow(new PosterDataError("Poster check has no date_close"));

        assertThatCode(() -> handler.handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        verifyNoInteractions(createOrderHandler);
    }

    @Test
    void handle_withLineThatBreaksItemRules_storesNoOrderAndDoesNotThrow() {
        givenPaidCheck(check(null, line("Шаурма", 0)));

        assertThatCode(() -> realHandler().handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        assertThat(repository.findByPosterCheckId(CHECK_ID)).isEmpty();
    }

    @Test
    void handle_withCheckWithoutLines_storesNoOrder() {
        givenPaidCheck(check(null));

        assertThatCode(() -> realHandler().handle(webhook("transaction", "changed")))
                .doesNotThrowAnyException();

        assertThat(repository.findByPosterCheckId(CHECK_ID)).isEmpty();
    }
}
