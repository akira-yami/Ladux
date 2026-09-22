package org.akira.ladux.procurement.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;

public record PurchaseOrderResponse(
        Integer id,
        Integer supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        Instant expectedDeliveryDate,
        BigDecimal totalAmount,
        String note,
        Integer createdById,
        Instant createdAt,
        Instant updatedAt,
        List<PurchaseOrderItemResponse> items
) implements Serializable {

    /** Ban tom tat (khong kem danh sach item) cho man hinh danh sach. */

}
