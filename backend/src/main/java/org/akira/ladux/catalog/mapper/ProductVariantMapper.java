package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.entity.ProductVariant;

public final class ProductVariantMapper {
    private ProductVariantMapper() {
    }

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getSku(),
                variant.getColor() != null ? ColorMapper.fromEntity(variant.getColor()) : null,
                variant.getRam(),
                variant.getRom(),
                variant.getPrice(),
                variant.getDiscountPrice(),
                variant.getStockQuantity(),
                variant.isActive()
        );
    }
}
