package org.akira.ladux.ordering.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.akira.ladux.ordering.enums.OrderStatus;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Status khong duoc de trong")
        OrderStatus status,

        @Size(max = 255, message = "TrackingNumber khong duoc vuot qua 255 ky tu")
        String trackingNumber
) {}
