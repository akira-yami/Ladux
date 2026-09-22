package org.akira.ladux.catalog.dto;

import java.util.List;

/**
 * Pagination contract independent from Spring Data and HTTP.
 */
public record ProductPage(
        List<ProductView> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}
