package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductVariantResponse(
    Integer id,
    Integer productId,
    String sku,
    ColorResponse color,
    String ram,
    String rom,
    BigDecimal price,
    BigDecimal discountPrice,
    int stockQuantity,
    boolean isActive
) implements Serializable {
    private static final long serialVersionUID = 1L;

}
