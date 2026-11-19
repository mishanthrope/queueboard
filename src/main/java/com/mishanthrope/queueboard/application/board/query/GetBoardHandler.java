package com.mishanthrope.queueboard.application.board.query;

import com.mishanthrope.queueboard.application.common.QueryHandler;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBoardHandler implements QueryHandler<GetBoard, BoardView> {

    private static final long MILLIS_PER_MINUTE = Duration.ofMinutes(1).toMillis();

    private final BoardReadRepository boardReadRepository;
    private final Clock clock;

    @Override
    public BoardView handle(GetBoard query) {
        Instant now = clock.instant();
        var preparing = new ArrayList<PreparingEntry>();
        var ready = new ArrayList<ReadyEntry>();
        for (var row : boardReadRepository.findActiveRows()) {
            if (row.status() == OrderStatus.READY) {
                ready.add(new ReadyEntry(row.displayNumber()));
            } else {
                preparing.add(new PreparingEntry(
                        row.displayNumber(), minutesLeft(now, row.estimatedReadyAt())));
            }
        }
        return new BoardView(preparing, ready);
    }

    private static long minutesLeft(Instant now, Instant estimatedReadyAt) {
        long millisLeft = Duration.between(now, estimatedReadyAt).toMillis();
        return millisLeft <= 0 ? 0 : Math.ceilDiv(millisLeft, MILLIS_PER_MINUTE);
    }
}
