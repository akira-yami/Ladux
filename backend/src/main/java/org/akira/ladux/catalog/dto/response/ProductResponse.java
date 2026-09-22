package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Integer id,
        BrandResponse brand,
        CategoryResponse category,
        String name,
        String slug,
        String description,
        String cpu,
        String gpu,
        String display,
        String battery,
        String weight,
        Integer numberOfFans,
        String os,
        boolean isActive,
        Instant createdAt,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        double averageRating,
        long reviewCount
) implements Serializable {
}
