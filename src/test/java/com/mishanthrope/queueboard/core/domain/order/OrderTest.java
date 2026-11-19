package com.mishanthrope.queueboard.core.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant ESTIMATE = NOW.plusSeconds(900);

    private static Order order(OrderStatus status) {
        return Order.reconstitute(
                1L, 100L, 7, status, "з собою", NOW.minusSeconds(600), ESTIMATE,
                NOW.minusSeconds(590), null, null, null, null,
                List.of(new OrderItem(1L, "Шаурма", 2, List.of("гострий соус"))));
    }

    @Test
    void reconstitute_keepsAllStoredState() {
        Instant started = NOW.minusSeconds(300);
        Order order = Order.reconstitute(
                5L, 100L, 7, OrderStatus.READY, "note", NOW.minusSeconds(600), ESTIMATE,
                NOW.minusSeconds(590), started, NOW.minusSeconds(60), null, null,
                List.of(new OrderItem(9L, "Кола", 1, List.of())));

        assertThat(order.getId()).isEqualTo(5L);
        assertThat(order.getPosterCheckId()).isEqualTo(100L);
        assertThat(order.getDisplayNumber()).isEqualTo(7);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(order.getComment()).isEqualTo("note");
        assertThat(order.getPaidAt()).isEqualTo(NOW.minusSeconds(600));
        assertThat(order.getEstimatedReadyAt()).isEqualTo(ESTIMATE);
        assertThat(order.getCreatedAt()).isEqualTo(NOW.minusSeconds(590));
        assertThat(order.getStartedAt()).isEqualTo(started);
        assertThat(order.getReadyAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(order.getItems()).extracting(OrderItem::productName).containsExactly("Кола");
    }

    @Test
    void getItems_isImmutable() {
        Order order = order(OrderStatus.NEW);

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void startCooking_fromNew_setsStatusAndStartedAt() {
        Order order = order(OrderStatus.NEW);

        order.startCooking(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(order.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void markReady_fromCooking_setsStatusAndReadyAt() {
        Order order = order(OrderStatus.COOKING);

        order.markReady(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(order.getReadyAt()).isEqualTo(NOW);
    }

    @Test
    void issue_fromReady_setsStatusAndIssuedAt() {
        Order order = order(OrderStatus.READY);

        order.issue(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ISSUED);
        assertThat(order.getIssuedAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_fromNew_setsStatusAndCancelledAt() {
        Order order = order(OrderStatus.NEW);

        order.cancel(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void restore_fromReady_clearsReadyAtAndKeepsStartedAt() {
        Instant started = NOW.minusSeconds(300);
        Order order = Order.reconstitute(
                1L, 100L, 7, OrderStatus.READY, null, NOW.minusSeconds(600), ESTIMATE,
                NOW.minusSeconds(590), started, NOW.minusSeconds(60), null, null, List.of());

        order.restore(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(order.getReadyAt()).isNull();
        assertThat(order.getCancelledAt()).isNull();
        assertThat(order.getStartedAt()).isEqualTo(started);
    }

    @Test
    void restore_fromCancelledWithoutStart_clearsCancelledAtAndSetsStartedAt() {
        Order order = order(OrderStatus.NEW);
        order.cancel(NOW.minusSeconds(60));

        order.restore(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(order.getCancelledAt()).isNull();
        assertThat(order.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void forbiddenAction_throwsAndLeavesOrderUntouched() {
        Order order = order(OrderStatus.NEW);

        assertThatThrownBy(() -> order.issue(NOW))
                .isInstanceOf(IllegalStatusTransitionError.class)
                .hasMessage("Cannot issue order in status NEW");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getIssuedAt()).isNull();
    }

    @Test
    void forbiddenActions_neverChangeTimestamps() {
        Order issued = order(OrderStatus.ISSUED);

        assertThatThrownBy(() -> issued.startCooking(NOW))
                .isInstanceOf(IllegalStatusTransitionError.class);
        assertThatThrownBy(() -> issued.markReady(NOW))
                .isInstanceOf(IllegalStatusTransitionError.class);
        assertThatThrownBy(() -> issued.cancel(NOW))
                .isInstanceOf(IllegalStatusTransitionError.class);
        assertThatThrownBy(() -> issued.restore(NOW))
                .isInstanceOf(IllegalStatusTransitionError.class);

        assertThat(issued.getStartedAt()).isNull();
        assertThat(issued.getReadyAt()).isNull();
        assertThat(issued.getCancelledAt()).isNull();
    }

    @Test
    void shiftEstimate_withPositiveDelta_movesEstimateForward() {
        Order order = order(OrderStatus.NEW);

        order.shiftEstimate(Duration.ofMinutes(5));

        assertThat(order.getEstimatedReadyAt()).isEqualTo(ESTIMATE.plusSeconds(300));
    }

    @Test
    void shiftEstimate_withNegativeDelta_movesEstimateBack() {
        Order order = order(OrderStatus.COOKING);

        order.shiftEstimate(Duration.ofMinutes(-10));

        assertThat(order.getEstimatedReadyAt()).isEqualTo(ESTIMATE.minusSeconds(600));
    }
}
