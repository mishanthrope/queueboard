package com.mishanthrope.queueboard.core.application.board.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class GetBoardHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private static GetBoardHandler handlerFor(BoardRow... rows) {
        BoardReadRepository fake = () -> List.of(rows);
        return new GetBoardHandler(fake, Clock.fixed(NOW, ZoneId.of("Europe/Kyiv")));
    }

    @Test
    void handle_groupsRowsByStatusKeepingOrder() {
        Instant later = NOW.plus(Duration.ofMinutes(10));

        BoardView board = handlerFor(
                new BoardRow(1, OrderStatus.READY, later),
                new BoardRow(2, OrderStatus.NEW, later),
                new BoardRow(3, OrderStatus.COOKING, later))
                .handle(new GetBoard());

        assertThat(board.ready()).containsExactly(new ReadyEntry(1));
        assertThat(board.preparing()).extracting(PreparingEntry::displayNumber)
                .containsExactly(2, 3);
    }

    @Test
    void handle_withNoRows_returnsEmptyGroups() {
        BoardView board = handlerFor().handle(new GetBoard());

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
    void handle_roundsMinutesLeftUpAndNeverBelowZero(long millisLeft, long expectedMinutes) {
        BoardView board = handlerFor(
                new BoardRow(1, OrderStatus.NEW, NOW.plusMillis(millisLeft)))
                .handle(new GetBoard());

        assertThat(board.preparing()).containsExactly(new PreparingEntry(1, expectedMinutes));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"NEW", "COOKING"})
    void handle_forPreparingOrder_usesStoredEstimate(OrderStatus status) {
        BoardView board = handlerFor(
                new BoardRow(5, status, NOW.plus(Duration.ofMinutes(9))))
                .handle(new GetBoard());

        assertThat(board.preparing()).containsExactly(new PreparingEntry(5, 9));
    }
}
