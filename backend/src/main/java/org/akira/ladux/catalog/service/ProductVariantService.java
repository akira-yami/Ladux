package org.akira.ladux.catalog.service;

import java.math.BigDecimal;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;

public interface ProductVariantService {
    ProductVariantResponse addProductVariant(Integer productId, Integer colorId, String ram, String rom, BigDecimal price, BigDecimal discountPrice, int stockQuantity, boolean active);

    ProductVariantResponse updateProductVariant(Integer id, Integer colorId, String ram, String rom, BigDecimal price, BigDecimal discountPrice, int stockQuantity, boolean active);

    void deleteProductVariant(Integer variantId);
}
