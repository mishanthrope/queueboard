package com.mishanthrope.queueboard.application.poster;

import java.time.Instant;
import java.util.List;

public record PosterCheck(Instant paidAt, String comment, List<PosterCheckLine> lines) {
}
