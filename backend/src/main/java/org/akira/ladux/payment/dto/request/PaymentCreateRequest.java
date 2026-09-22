package org.akira.ladux.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.akira.ladux.payment.enums.PaymentProvider;

// Giúp khách tạo thanh toán, thanh toán lại, linh hoạt hơn
public record PaymentCreateRequest(
        @NotNull(message = "OrderId khong duoc de trong")
        @Positive(message = "OrderId phai la so duong")
        Integer orderId,

        @NotNull(message = "Provider khong duoc de trong")
        PaymentProvider provider
) {}
