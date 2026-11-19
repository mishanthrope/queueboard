package com.mishanthrope.queueboard.core.presentation.board;

import com.mishanthrope.queueboard.core.application.board.query.BoardView;
import com.mishanthrope.queueboard.core.application.board.query.GetBoard;
import com.mishanthrope.queueboard.core.application.board.query.GetBoardHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final GetBoardHandler getBoard;

    @GetMapping
    public BoardView getBoard() {
        return getBoard.handle(new GetBoard());
    }
}
