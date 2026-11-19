package com.mishanthrope.queueboard.application.board.query;

import java.util.List;

public interface BoardReadRepository {

    List<BoardRow> findActiveRows();
}
