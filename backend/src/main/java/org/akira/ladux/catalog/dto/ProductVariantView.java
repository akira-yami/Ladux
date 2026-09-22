package org.akira.ladux.catalog.dto;

import java.math.BigDecimal;

public record ProductVariantView(
    Integer id,
    Integer productId,
    String sku,
    Integer colorId,
    String colorName,
    String colorHexCode,
    String ram,
    String rom,
    BigDecimal price,
    BigDecimal discountPrice,
    boolean active
) {
}
