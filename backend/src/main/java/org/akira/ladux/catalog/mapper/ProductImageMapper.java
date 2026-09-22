package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.ProductImageResponse;
import org.akira.ladux.catalog.entity.ProductImage;

public final class ProductImageMapper {
    private ProductImageMapper() {
    }

    public static ProductImageResponse fromEntity(ProductImage image) {
        if (image == null) {
            return null;
        }
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl()
        );
    }
}
