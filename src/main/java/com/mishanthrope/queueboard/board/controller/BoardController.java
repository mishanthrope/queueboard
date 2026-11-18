package com.mishanthrope.queueboard.board.controller;

import com.mishanthrope.queueboard.board.dto.BoardResponse;
import com.mishanthrope.queueboard.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public BoardResponse getBoard() {
        return boardService.getBoard();
    }
}
