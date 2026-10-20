package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Mot san pham goi y trong Smart Search dropdown.
 * Gia (price/discountPrice) lay tu bien the hop le (active) re nhat con hang.
 * imageUrl va gia deu lay tu nguon su that catalog — khong bia.
 */
public record SuggestionProductResponse(
        Integer productId,
        Integer variantId,
        String name,
        String slug,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        boolean available
) implements Serializable {
}
