package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;

public record CartItemResponse(
        Integer id,
        ProductResponse product,
        ProductVariantResponse productVariant,
        int quantity
) implements Serializable {
}
