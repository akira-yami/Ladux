package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.request.ProductRequest;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    PageResponse<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(int id);

    ProductResponse getProductBySlug(String slug);

    PageResponse<ProductResponse> getProductsByBrandId(int brandId, Pageable pageable);

    PageResponse<ProductResponse> getProductsByCategoryId(int categoryId, Pageable pageable);

    PageResponse<ProductResponse> searchProducts(String search, Pageable pageable);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(int id, ProductRequest request);

    void deleteProductById(int id);

    ProductVariantResponse getProductVariantById(Integer variantId);
}
