package org.akira.ladux.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundAttemptResponse(Long id, Integer orderId, Integer paymentId, String requestId,
        BigDecimal amount, String status, String reason, Instant createdAt, Instant updatedAt) {
}
