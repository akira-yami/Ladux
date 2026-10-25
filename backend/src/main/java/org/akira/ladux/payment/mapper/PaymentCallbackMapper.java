package org.akira.ladux.payment.mapper;

import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;

public final class PaymentCallbackMapper {
    private PaymentCallbackMapper() {
    }

    public static PaymentCallbackResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentCallbackResponse(
                payment.getId(),
                payment.getOrder() == null ? null : payment.getOrder().getId(),
                payment.getProvider(),
                payment.getMerchantTxnRef(),
                payment.getTransactionNo(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentUrl(),
                payment.getCreatedAt(),
                payment.getOrder() == null ? null : payment.getOrder().getStatus(),
                payment.getOrder() == null ? null : payment.getOrder().getPaymentExpiresAt(),
                payment.getOrder() != null
                        && payment.getOrder().getStatus() == org.akira.ladux.ordering.enums.OrderStatus.PENDING
                        && payment.getStatus() == org.akira.ladux.payment.enums.PaymentStatus.FAILED
                        && (payment.getOrder().getPaymentExpiresAt() == null
                            || payment.getOrder().getPaymentExpiresAt().isAfter(java.time.Instant.now())),
                payment.isRequiresReview() || payment.getStatus() == org.akira.ladux.payment.enums.PaymentStatus.SUCCESS
                        && payment.getOrder() != null
                        && payment.getOrder().getStatus() == org.akira.ladux.ordering.enums.OrderStatus.CANCELLED
        );
    }
}
