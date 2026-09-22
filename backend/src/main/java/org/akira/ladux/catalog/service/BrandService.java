package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.request.BrandRequest;
import org.akira.ladux.catalog.dto.response.BrandResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BrandService {
    PageResponse<BrandResponse> getAllBrands(Pageable pageable);

    BrandResponse getBrandById(int id);

    BrandResponse getBrandByName(String name);

    BrandResponse getBrandBySlug(String slug);

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(int id, BrandRequest brand);

    void deleteBrandById(int id);
}
