package com.mishanthrope.queueboard.application.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(
                orderRepository, Clock.fixed(NOW, ZoneId.of("Europe/Kyiv")));
    }

    private static Order order(int displayNumber, OrderStatus status, Instant estimatedReadyAt) {
        return Order.reconstitute(
                (long) displayNumber, 1000L + displayNumber, displayNumber, status, null,
                NOW.minusSeconds(600), estimatedReadyAt, NOW.minusSeconds(590), null, null,
                null, null, List.of());
    }

    @Test
    void getBoard_groupsOrdersByStatus() {
        Instant later = NOW.plus(Duration.ofMinutes(10));
        when(orderRepository.findActive()).thenReturn(List.of(
                order(1, OrderStatus.READY, later),
                order(2, OrderStatus.NEW, later),
                order(3, OrderStatus.COOKING, later)));

        BoardResponse board = boardService.getBoard();

        assertThat(board.ready()).containsExactly(new ReadyEntry(1));
        assertThat(board.preparing()).extracting(PreparingEntry::displayNumber)
                .containsExactly(2, 3);
    }

    @Test
    void getBoard_withNoOrders_returnsEmptyGroups() {
        when(orderRepository.findActive()).thenReturn(List.of());

        BoardResponse board = boardService.getBoard();

        assertThat(board.preparing()).isEmpty();
        assertThat(board.ready()).isEmpty();
    }

    @ParameterizedTest(name = "{0} ms left -> {1} min")
    @CsvSource({
            "240000, 4",
            "241000, 5",
            "1, 1",
            "60000, 1",
            "60001, 2",
            "0, 0",
            "-1, 0",
            "-600000, 0"})
    void getBoard_roundsMinutesLeftUpAndNeverBelowZero(long millisLeft, long expectedMinutes) {
        when(orderRepository.findActive()).thenReturn(List.of(
                order(1, OrderStatus.NEW, NOW.plusMillis(millisLeft))));

        assertThat(boardService.getBoard().preparing())
                .containsExactly(new PreparingEntry(1, expectedMinutes));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"NEW", "COOKING"})
    void getBoard_forActiveOrder_usesStoredEstimate(OrderStatus status) {
        when(orderRepository.findActive()).thenReturn(List.of(
                order(5, status, NOW.plus(Duration.ofMinutes(9)))));

        assertThat(boardService.getBoard().preparing())
                .containsExactly(new PreparingEntry(5, 9));
    }
}
