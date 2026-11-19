package com.mishanthrope.queueboard.application.board;

import java.util.List;

public record BoardResponse(List<PreparingEntry> preparing, List<ReadyEntry> ready) {
}
