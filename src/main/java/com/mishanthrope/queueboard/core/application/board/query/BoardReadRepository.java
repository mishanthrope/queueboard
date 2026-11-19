package com.mishanthrope.queueboard.core.application.board.query;

import java.util.List;

public interface BoardReadRepository {

    List<BoardRow> findActiveRows();
}
