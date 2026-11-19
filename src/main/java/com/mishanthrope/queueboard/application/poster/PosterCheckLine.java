package com.mishanthrope.queueboard.application.poster;

import java.util.List;

public record PosterCheckLine(String productName, int quantity, List<String> modifiers) {
}
