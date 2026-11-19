package com.mishanthrope.queueboard.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    @Test
    void getById_returnsTheStoredOrder() {
        Order saved = repository.save(Order.reconstitute(
                null, 100L, 1, OrderStatus.NEW, null, NOW, NOW, NOW, null, null, null, null,
                List.of()));

        assertThat(repository.getById(saved.getId())).isSameAs(saved);
    }

    @Test
    void getById_withUnknownId_throwsOrderNotFound() {
        assertThatThrownBy(() -> repository.getById(99L))
                .isInstanceOf(OrderNotFoundError.class)
                .hasMessage("Order 99 not found");
    }
}
