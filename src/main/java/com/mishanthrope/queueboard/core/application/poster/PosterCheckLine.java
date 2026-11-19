package com.mishanthrope.queueboard.core.application.poster;

import java.util.List;

public record PosterCheckLine(String productName, int quantity, List<String> modifiers) {
}
