package com.mishanthrope.queueboard.application.order.command;

import static com.mishanthrope.queueboard.application.order.OrderFixtures.seed;
import static org.assertj.core.api.Assertions.assertThat;

import com.mishanthrope.queueboard.domain.order.InMemoryOrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShiftPrepTimeHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant ESTIMATE = NOW.plusSeconds(600);

    private InMemoryOrderRepository repository;
    private ShiftPrepTimeHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        handler = new ShiftPrepTimeHandler(repository);
    }

    private Instant estimate(long id) {
        return repository.findById(id).orElseThrow().getEstimatedReadyAt();
    }

    @Test
    void handle_movesEstimateOfNewAndCookingOrders() {
        long fresh = seed(repository, 1, 1, OrderStatus.NEW, NOW, ESTIMATE).getId();
        long cooking = seed(repository, 2, 2, OrderStatus.COOKING, NOW, ESTIMATE).getId();

        handler.handle(new ShiftPrepTime(5));

        assertThat(estimate(fresh)).isEqualTo(ESTIMATE.plusSeconds(300));
        assertThat(estimate(cooking)).isEqualTo(ESTIMATE.plusSeconds(300));
    }

    @Test
    void handle_withNegativeDelta_movesEstimateBack() {
        long cooking = seed(repository, 2, 2, OrderStatus.COOKING, NOW, ESTIMATE).getId();

        handler.handle(new ShiftPrepTime(-10));

        assertThat(estimate(cooking)).isEqualTo(ESTIMATE.minusSeconds(600));
    }

    @Test
    void handle_doesNotMoveReadyIssuedAndCancelledOrders() {
        List<Long> ids = List.of(
                seed(repository, 1, 1, OrderStatus.READY, NOW, ESTIMATE).getId(),
                seed(repository, 2, 2, OrderStatus.ISSUED, NOW, ESTIMATE).getId(),
                seed(repository, 3, 3, OrderStatus.CANCELLED, NOW, ESTIMATE).getId());

        handler.handle(new ShiftPrepTime(5));

        for (var id : ids) {
            assertThat(estimate(id)).isEqualTo(ESTIMATE);
        }
    }

    @Test
    void handle_withEmptyQueue_doesNothing() {
        handler.handle(new ShiftPrepTime(5));

        assertThat(repository.findInQueue()).isEmpty();
    }
}
