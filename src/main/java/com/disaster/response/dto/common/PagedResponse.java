package com.disaster.response.dto.common;

import org.springframework.data.domain.Page;
import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    public static <T, R> PagedResponse<R> from(Page<T> page, java.util.function.Function<T, R> mapper) {
        List<R> mappedContent = page.getContent().stream().map(mapper).toList();
        return new PagedResponse<>(
            mappedContent,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
