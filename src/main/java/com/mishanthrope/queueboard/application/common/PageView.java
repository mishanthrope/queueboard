package com.mishanthrope.queueboard.application.common;

import java.util.List;

public record PageView<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageView {
        content = List.copyOf(content);
    }
}
