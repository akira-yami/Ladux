package org.akira.ladux.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;

public record PaymentCallbackRequest(
        @NotNull(message = "OrderId khong duoc de trong")
        @Positive(message = "OrderId phai la so duong")
        Integer orderId,

        @NotNull(message = "Provider khong duoc de trong")
        PaymentProvider provider,

        @Size(max = 255, message = "TransactionNo khong duoc vuot qua 255 ky tu")
        String transactionNo,

        PaymentStatus status
) {}
