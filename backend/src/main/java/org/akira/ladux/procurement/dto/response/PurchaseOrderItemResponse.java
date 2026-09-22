package org.akira.ladux.procurement.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Integer id,
        Integer productVariantId,
        String productName,
        Integer quantity,
        BigDecimal costPrice,
        Integer receivedQuantity,
        String note
) implements Serializable {
}
