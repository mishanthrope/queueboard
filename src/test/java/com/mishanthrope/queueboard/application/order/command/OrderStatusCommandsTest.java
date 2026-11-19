package com.mishanthrope.queueboard.application.order.command;

import static com.mishanthrope.queueboard.application.order.OrderFixtures.seed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mishanthrope.queueboard.domain.order.IllegalStatusTransitionError;
import com.mishanthrope.queueboard.domain.order.InMemoryOrderRepository;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderNotFoundError;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderStatusCommandsTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant ESTIMATE = NOW.plusSeconds(600);

    private InMemoryOrderRepository repository;
    private StartCookingHandler startCooking;
    private MarkReadyHandler markReady;
    private IssueOrderHandler issueOrder;
    private CancelOrderHandler cancelOrder;
    private RestoreOrderHandler restoreOrder;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        Clock clock = Clock.fixed(NOW, ZoneId.of("Europe/Kyiv"));
        startCooking = new StartCookingHandler(repository, clock);
        markReady = new MarkReadyHandler(repository, clock);
        issueOrder = new IssueOrderHandler(repository, clock);
        cancelOrder = new CancelOrderHandler(repository, clock);
        restoreOrder = new RestoreOrderHandler(repository, clock);
    }

    private long seeded(OrderStatus status) {
        return seed(repository, 1000, 1, status, NOW.minusSeconds(590), ESTIMATE).getId();
    }

    private Order stored(long id) {
        return repository.findById(id).orElseThrow();
    }

    @Test
    void startCooking_fromNew_savesCookingOrderWithStartedAt() {
        long id = seeded(OrderStatus.NEW);

        startCooking.handle(new StartCooking(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(stored(id).getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void markReady_fromCooking_savesReadyOrderWithReadyAt() {
        long id = seeded(OrderStatus.COOKING);

        markReady.handle(new MarkReady(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(stored(id).getReadyAt()).isEqualTo(NOW);
    }

    @Test
    void issue_fromReady_savesIssuedOrderWithIssuedAt() {
        long id = seeded(OrderStatus.READY);

        issueOrder.handle(new IssueOrder(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.ISSUED);
        assertThat(stored(id).getIssuedAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_fromNew_savesCancelledOrderWithCancelledAt() {
        long id = seeded(OrderStatus.NEW);

        cancelOrder.handle(new CancelOrder(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stored(id).getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void restore_fromCancelledWithoutStart_savesCookingOrderStartedNow() {
        long id = seeded(OrderStatus.NEW);
        cancelOrder.handle(new CancelOrder(id));

        restoreOrder.handle(new RestoreOrder(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(stored(id).getCancelledAt()).isNull();
        assertThat(stored(id).getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void restore_fromReady_clearsReadyAtAndKeepsStartedAt() {
        long id = seeded(OrderStatus.NEW);
        startCooking.handle(new StartCooking(id));
        markReady.handle(new MarkReady(id));

        restoreOrder.handle(new RestoreOrder(id));

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(stored(id).getReadyAt()).isNull();
        assertThat(stored(id).getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void command_forbiddenInCurrentStatus_throwsAndLeavesOrderUntouched() {
        long id = seeded(OrderStatus.NEW);

        assertThatThrownBy(() -> issueOrder.handle(new IssueOrder(id)))
                .isInstanceOf(IllegalStatusTransitionError.class)
                .hasMessage("Cannot issue order in status NEW");

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(stored(id).getIssuedAt()).isNull();
    }

    @Test
    void cancel_ofIssuedOrder_throwsAndKeepsIssued() {
        long id = seeded(OrderStatus.ISSUED);

        assertThatThrownBy(() -> cancelOrder.handle(new CancelOrder(id)))
                .isInstanceOf(IllegalStatusTransitionError.class);

        assertThat(stored(id).getStatus()).isEqualTo(OrderStatus.ISSUED);
    }

    @Test
    void command_doesNotTouchOtherOrders() {
        long first = seeded(OrderStatus.NEW);
        long second = seed(repository, 1001, 2, OrderStatus.NEW, NOW, ESTIMATE).getId();

        startCooking.handle(new StartCooking(first));

        assertThat(stored(second).getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(stored(second).getStartedAt()).isNull();
    }

    static Stream<Arguments> commandsOnUnknownOrder() {
        return Stream.of(
                command("StartCooking", t -> t.startCooking.handle(new StartCooking(99L))),
                command("MarkReady", t -> t.markReady.handle(new MarkReady(99L))),
                command("IssueOrder", t -> t.issueOrder.handle(new IssueOrder(99L))),
                command("CancelOrder", t -> t.cancelOrder.handle(new CancelOrder(99L))),
                command("RestoreOrder", t -> t.restoreOrder.handle(new RestoreOrder(99L))));
    }

    private static Arguments command(String name, Consumer<OrderStatusCommandsTest> call) {
        return Arguments.of(name, call);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("commandsOnUnknownOrder")
    void command_withUnknownId_throwsOrderNotFound(
            String name, Consumer<OrderStatusCommandsTest> call) {
        assertThatThrownBy(() -> call.accept(this))
                .isInstanceOf(OrderNotFoundError.class)
                .hasMessage("Order 99 not found");
    }
}
