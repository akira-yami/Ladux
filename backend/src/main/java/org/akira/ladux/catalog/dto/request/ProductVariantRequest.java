package org.akira.ladux.catalog.dto.request;

import java.math.BigDecimal;

public record ProductVariantRequest(
        Integer id,
        Integer productId,
        Integer colorId,
        String ram,
        String rom,
        BigDecimal price,
        BigDecimal discountPrice,
        int stockQuantity,
        boolean isActive
) {
}
