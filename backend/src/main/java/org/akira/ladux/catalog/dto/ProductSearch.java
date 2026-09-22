package org.akira.ladux.catalog.dto;

/**
 * Product browse criteria. Sort fields are deliberately limited by the
 * persistence adapter rather than accepting arbitrary client property paths.
 */
public record ProductSearch(
        String text,
        Integer brandId,
        Integer categoryId,
        int pageNumber,
        int pageSize,
        String sortBy,
        boolean ascending
) {
    public ProductSearch {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
