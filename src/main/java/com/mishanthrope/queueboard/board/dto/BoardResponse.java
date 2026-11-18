package com.mishanthrope.queueboard.board.dto;

import java.util.List;

public record BoardResponse(List<PreparingEntry> preparing, List<ReadyEntry> ready) {
}
