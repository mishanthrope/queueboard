package com.mishanthrope.queueboard.order.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mishanthrope.queueboard.order.exception.IllegalStatusTransitionException;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderStatusTest {

    private enum Action {
        START(OrderStatus::startCooking),
        READY(OrderStatus::markReady),
        ISSUE(OrderStatus::issue),
        CANCEL(OrderStatus::cancel),
        RESTORE(OrderStatus::restore);

        private final UnaryOperator<OrderStatus> call;

        Action(UnaryOperator<OrderStatus> call) {
            this.call = call;
        }

        OrderStatus applyTo(OrderStatus status) {
            return call.apply(status);
        }
    }

    private static final Map<OrderStatus, Map<Action, OrderStatus>> ALLOWED = Map.of(
            OrderStatus.NEW, Map.of(
                    Action.START, OrderStatus.COOKING,
                    Action.CANCEL, OrderStatus.CANCELLED),
            OrderStatus.COOKING, Map.of(
                    Action.READY, OrderStatus.READY,
                    Action.CANCEL, OrderStatus.CANCELLED),
            OrderStatus.READY, Map.of(
                    Action.ISSUE, OrderStatus.ISSUED,
                    Action.CANCEL, OrderStatus.CANCELLED,
                    Action.RESTORE, OrderStatus.COOKING),
            OrderStatus.ISSUED, Map.of(),
            OrderStatus.CANCELLED, Map.of(Action.RESTORE, OrderStatus.COOKING));

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED.entrySet().stream().flatMap(byStatus ->
                byStatus.getValue().entrySet().stream().map(byAction -> Arguments.of(
                        byStatus.getKey(), Named.of(byAction.getKey().name(), byAction.getKey()),
                        byAction.getValue())));
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(OrderStatus.values()).flatMap(status ->
                Stream.of(Action.values())
                        .filter(action -> !ALLOWED.get(status).containsKey(action))
                        .map(action -> Arguments.of(
                                status, Named.of(action.name(), action))));
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("allowedTransitions")
    void action_whenAllowed_returnsNextStatus(
            OrderStatus status, Action action, OrderStatus expected) {
        assertThat(action.applyTo(status)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} --{1}--> rejected")
    @MethodSource("forbiddenTransitions")
    void action_whenForbidden_throwsIllegalStatusTransition(OrderStatus status, Action action) {
        assertThatThrownBy(() -> action.applyTo(status))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining(status.name());
    }

    @Test
    void issued_allowsNoAction() {
        for (Action action : Action.values()) {
            assertThatThrownBy(() -> action.applyTo(OrderStatus.ISSUED))
                    .isInstanceOf(IllegalStatusTransitionException.class);
        }
    }
}
