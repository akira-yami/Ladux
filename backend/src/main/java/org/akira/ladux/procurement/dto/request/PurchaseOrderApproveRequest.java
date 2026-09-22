package org.akira.ladux.procurement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record PurchaseOrderApproveRequest(
        @Future(message = "Ngay giao du kien phai la mot ngay trong tuong lai")
        Instant expectedDeliveryDate,

        @Size(max = 100, message = "Ma van don khong duoc vuot qua 100 ky tu")
        String trackingNumber,

        @Size(max = 500, message = "Ghi chu khong duoc vuot qua 500 ky tu")
        String note
) {
}
