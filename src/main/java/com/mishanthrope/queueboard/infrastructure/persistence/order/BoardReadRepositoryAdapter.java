package com.mishanthrope.queueboard.infrastructure.persistence.order;

import com.mishanthrope.queueboard.application.board.query.BoardReadRepository;
import com.mishanthrope.queueboard.application.board.query.BoardRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardReadRepositoryAdapter implements BoardReadRepository {

    private final SpringDataOrderRepository springDataRepository;

    @Override
    public List<BoardRow> findActiveRows() {
        return springDataRepository.findBoardRows(OrderStatusSets.ACTIVE);
    }
}
