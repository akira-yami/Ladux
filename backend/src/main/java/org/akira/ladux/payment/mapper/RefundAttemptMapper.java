package org.akira.ladux.payment.mapper;

import org.akira.ladux.payment.dto.response.RefundAttemptResponse;
import org.akira.ladux.payment.entity.RefundAttempt;

public final class RefundAttemptMapper {
    private RefundAttemptMapper() {}

    public static RefundAttemptResponse fromEntity(RefundAttempt attempt) {
        return new RefundAttemptResponse(attempt.getId(), attempt.getOrderId(), attempt.getPaymentId(),
                attempt.getRequestId(), attempt.getAmount(), attempt.getStatus().name(), attempt.getReason(),
                attempt.getCreatedAt(), attempt.getUpdatedAt());
    }
}
