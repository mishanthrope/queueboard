package com.mishanthrope.queueboard.application.board.query;

import java.util.List;

public record BoardView(List<PreparingEntry> preparing, List<ReadyEntry> ready) {
}
