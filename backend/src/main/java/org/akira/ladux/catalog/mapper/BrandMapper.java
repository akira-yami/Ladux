package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.BrandResponse;
import org.akira.ladux.catalog.entity.Brand;

public final class BrandMapper {
    private BrandMapper() {
    }

    public static BrandResponse fromEntity(Brand brand) {
        if (brand == null) {
            return null;
        }
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getLogoUrl(),
                brand.getSlug());
    }
}
