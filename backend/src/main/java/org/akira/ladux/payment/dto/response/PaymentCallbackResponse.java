package org.akira.ladux.payment.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;

public record PaymentCallbackResponse(
        Integer id,
        Integer orderId,
        PaymentProvider provider,
        String merchantTxnRef,
        String transactionNo,
        BigDecimal amount,
        PaymentStatus status,
        String paymentUrl,
        Instant createdAt
) implements Serializable {
}
