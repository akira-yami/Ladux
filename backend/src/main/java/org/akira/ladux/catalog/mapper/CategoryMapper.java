package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.CategoryResponse;
import org.akira.ladux.catalog.entity.Category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getImageUrl()
        );
    }
}
