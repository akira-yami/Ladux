package org.akira.ladux.shared.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public int getNumber() {
        return pageNumber;
    }

    public int getSize() {
        return pageSize;
    }

    /**
     * Factory method tiện lợi để convert trực tiếp từ Spring Data Page
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}