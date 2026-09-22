package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import org.akira.ladux.catalog.dto.response.ProductResponse;

public record OrderItemResponse(
        Integer id,
        Integer orderId,
        ProductResponse product,
        Integer productVariantId,
        int quantity,
        BigDecimal priceAtPurchase
) implements Serializable {
}
