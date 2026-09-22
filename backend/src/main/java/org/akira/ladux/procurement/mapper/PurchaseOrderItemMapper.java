package org.akira.ladux.procurement.mapper;

import org.akira.ladux.procurement.dto.response.PurchaseOrderItemResponse;
import org.akira.ladux.procurement.entity.PurchaseOrderItem;

public final class PurchaseOrderItemMapper {
    private PurchaseOrderItemMapper() {
    }

    public static PurchaseOrderItemResponse fromEntity(PurchaseOrderItem item) {
        if (item == null) {
            return null;
        }
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getProductVariant() == null ? null : item.getProductVariant().getId(),
                item.getProductVariant() == null ? null : item.getProductVariant().getProduct().getName(),
                item.getQuantity(),
                item.getCostPrice(),
                item.getReceivedQuantity(),
                item.getNote()
        );
    }
}
