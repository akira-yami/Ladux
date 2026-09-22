package org.akira.ladux.procurement.mapper;

import java.util.List;
import org.akira.ladux.procurement.dto.response.PurchaseOrderResponse;
import org.akira.ladux.procurement.entity.PurchaseOrder;

public final class PurchaseOrderMapper {
    private PurchaseOrderMapper() {
    }

    public static PurchaseOrderResponse fromEntity(PurchaseOrder po) {
        if (po == null) {
            return null;
        }
        return new PurchaseOrderResponse(
                po.getId(),
                po.getSupplier() == null ? null : po.getSupplier().getId(),
                po.getSupplier() == null ? null : po.getSupplier().getName(),
                po.getStatus(),
                po.getExpectedDeliveryDate(),
                po.getTotalAmount(),
                po.getNote(),
                po.getCreatedBy() == null ? null : po.getCreatedBy().getId(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getItems() == null ? List.of() : po.getItems().stream()
                        .map(PurchaseOrderItemMapper::fromEntity)
                        .toList()
        );
    }

    public static PurchaseOrderResponse summaryFromEntity(PurchaseOrder po) {
        if (po == null) {
            return null;
        }
        return new PurchaseOrderResponse(
                po.getId(),
                po.getSupplier() == null ? null : po.getSupplier().getId(),
                po.getSupplier() == null ? null : po.getSupplier().getName(),
                po.getStatus(),
                po.getExpectedDeliveryDate(),
                po.getTotalAmount(),
                po.getNote(),
                po.getCreatedBy() == null ? null : po.getCreatedBy().getId(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                List.of()
        );
    }
}
