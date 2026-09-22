package org.akira.ladux.procurement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;

public record PurchaseOrderStatusUpdateRequest(
        @NotNull(message = "Trang thai khong duoc de trong")
        PurchaseOrderStatus status,
        @Size(max = 500, message = "Ly do huy khong duoc vuot qua 500 ky tu")
        String cancelReason
) {}
